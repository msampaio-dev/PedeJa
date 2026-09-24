package com.pedeja.pagamento.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pedeja.pagamento.dto.PagamentoResponse;
import com.pedeja.pagamento.entity.Pagamento;
import com.pedeja.pagamento.entity.StatusPagamento;
import com.pedeja.pagamento.exception.PagamentoNaoEncontradoException;
import com.pedeja.pagamento.exception.PedidoNaoAguardaPagamentoException;
import com.pedeja.pagamento.gateway.GatewayPagamento;
import com.pedeja.pagamento.gateway.GatewayPagamento.CobrancaCriada;
import com.pedeja.pagamento.gateway.GatewayPagamento.NovaCobranca;
import com.pedeja.pagamento.repository.PagamentoRepository;
import com.pedeja.pagamento.repository.WebhookEventoRepository;
import com.pedeja.pagamento.webhook.EventoPagamento;
import com.pedeja.pedido.entity.Pedido;
import com.pedeja.pedido.entity.StatusPedido;
import com.pedeja.pedido.exception.PedidoNaoEncontradoException;
import com.pedeja.pedido.repository.PedidoRepository;
import com.pedeja.pedido.service.PedidoService;

@Service
public class PagamentoService {

	private static final Logger log = LoggerFactory.getLogger(PagamentoService.class);

	private final PagamentoRepository pagamentoRepository;
	private final WebhookEventoRepository webhookEventoRepository;
	private final PedidoRepository pedidoRepository;
	private final PedidoService pedidoService;
	private final GatewayPagamento gateway;
	private final Clock clock;
	private final Duration validadeCobranca;

	public PagamentoService(
			PagamentoRepository pagamentoRepository,
			WebhookEventoRepository webhookEventoRepository,
			PedidoRepository pedidoRepository,
			PedidoService pedidoService,
			GatewayPagamento gateway,
			Clock clock,
			@Value("${app.pagamento.validade-cobranca-minutos}") long validadeCobrancaMinutos
	) {
		this.pagamentoRepository = pagamentoRepository;
		this.webhookEventoRepository = webhookEventoRepository;
		this.pedidoRepository = pedidoRepository;
		this.pedidoService = pedidoService;
		this.gateway = gateway;
		this.clock = clock;
		this.validadeCobranca = Duration.ofMinutes(validadeCobrancaMinutos);
	}

	/**
	 * Gera a cobrança Pix do pedido. Chamar de novo enquanto a cobrança está
	 * válida devolve a mesma, em vez de criar outra: o cliente que recarrega a
	 * tela continua com o mesmo código Pix.
	 *
	 * A chamada ao gateway acontece com a linha do pedido travada. Com um gateway
	 * real isso pede timeout curto, para uma lentidão lá fora não segurar a trava.
	 */
	@Transactional
	public PagamentoResponse iniciar(Long clienteId, Long pedidoId) {
		Instant agora = Instant.now(clock);
		Pedido pedido = pedidoService.travarDoCliente(clienteId, pedidoId);

		if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
			throw new PedidoNaoAguardaPagamentoException();
		}

		var pendente = pagamentoRepository.findByPedidoIdAndStatus(pedidoId, StatusPagamento.PENDENTE);
		if (pendente.isPresent()) {
			if (pendente.get().aguardandoPagamento(agora)) {
				return PagamentoResponse.from(pendente.get(), agora);
			}
			// Libera o índice uk_pagamentos_pedido_pendente para a cobrança nova.
			pendente.get().expirar(agora);
			pagamentoRepository.flush();
		}

		Instant expiraEm = agora.plus(validadeCobranca);
		CobrancaCriada cobranca = gateway.criarCobranca(new NovaCobranca("pedido-" + pedidoId, pedido.getTotal(), expiraEm));

		Pagamento pagamento = new Pagamento(
				pedidoId, pedido.getTotal(), cobranca.id(), cobranca.pixCopiaECola(), expiraEm, agora);
		return PagamentoResponse.from(pagamentoRepository.save(pagamento), agora);
	}

	@Transactional(readOnly = true)
	public PagamentoResponse buscarUltimo(Long clienteId, Long pedidoId) {
		pedidoRepository.findByIdAndClienteId(pedidoId, clienteId).orElseThrow(PedidoNaoEncontradoException::new);
		return pagamentoRepository.findFirstByPedidoIdOrderByIdDesc(pedidoId)
				.map(pagamento -> PagamentoResponse.from(pagamento, Instant.now(clock)))
				.orElseThrow(PagamentoNaoEncontradoException::new);
	}

	/**
	 * Processa a notificação do gateway. O gateway entrega "pelo menos uma vez":
	 * a mesma notificação pode chegar repetida, e só a primeira tem efeito.
	 *
	 * Tudo roda numa transação só. Se algo falhar no meio, o registro do evento
	 * volta junto, o webhook responde 500 e o gateway tenta de novo.
	 */
	@Transactional
	public void processar(EventoPagamento evento) {
		Instant agora = Instant.now(clock);

		if (!webhookEventoRepository.registrarSeNovo(evento.eventoId(), agora)) {
			log.info("Evento {} já processado; entrega repetida ignorada", evento.eventoId());
			return;
		}

		Pagamento pagamento = pagamentoRepository.findByGatewayCobrancaId(evento.cobrancaId()).orElse(null);
		if (pagamento == null) {
			// Responder erro faria o gateway insistir para sempre numa cobrança que não é nossa.
			log.warn("Evento {} para cobrança desconhecida {}; ignorado", evento.eventoId(), evento.cobrancaId());
			return;
		}

		// Trava o pedido antes de mexer no pagamento: a mesma ordem do cancelamento,
		// para as duas operações nunca se travarem uma à outra (deadlock).
		Pedido pedido = pedidoRepository.buscarParaAtualizar(pagamento.getPedidoId()).orElseThrow();

		if (pagamento.getStatus() == StatusPagamento.APROVADO || pagamento.getStatus() == StatusPagamento.RECUSADO) {
			log.info("Pagamento {} já estava {}; evento {} sem efeito", pagamento.getId(), pagamento.getStatus(), evento.eventoId());
			return;
		}

		if (evento.resultado() == EventoPagamento.Resultado.RECUSADO) {
			pagamento.recusar(agora);
			return;
		}

		pagamento.aprovar(agora);

		if (pedido.getStatus() == StatusPedido.AGUARDANDO_PAGAMENTO) {
			pedido.mudarStatus(StatusPedido.PAGO, agora);
			pedidoRepository.save(pedido);
		} else {
			// O dinheiro entrou, mas o pedido não espera mais pagamento (foi
			// cancelado depois que a cobrança venceu, ou já foi pago por outra).
			// Aqui entraria o estorno automático; no projeto fica o registro.
			log.warn("Pagamento {} aprovado com o pedido {} em {}; estorno necessário",
					pagamento.getId(), pedido.getId(), pedido.getStatus());
		}
	}
}
