package com.pedeja.simulacao;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.pedeja.pedido.entity.Pedido;
import com.pedeja.pedido.entity.StatusPedido;
import com.pedeja.pedido.repository.PedidoRepository;

/**
 * Faz o papel do restaurante e do entregador na demonstração: pedido pago que
 * fica parado avança sozinho até ENTREGUE.
 *
 * O prazo conta a partir da última mudança de status (atualizado_em). Se o
 * restaurante agir antes pelo painel, o simulador só espera o próximo passo.
 * Recusa e cancelamento não são simulados: PAGO sempre vira ACEITO.
 *
 * As mudanças passam pela máquina de estados e pelo save() do pedido, então
 * geram histórico, outbox, notificação e SSE como qualquer outra.
 */
@Component
@Lazy(false)
@ConditionalOnProperty(name = "app.simulacao-entrega.enabled", havingValue = "true")
public class SimuladorEntrega {

	private static final Logger log = LoggerFactory.getLogger(SimuladorEntrega.class);

	private record Passo(StatusPedido de, StatusPedido para, Duration espera) {
	}

	private final List<Passo> passos;
	private final PedidoRepository pedidoRepository;
	private final TransactionTemplate transactionTemplate;
	private final Clock clock;

	public SimuladorEntrega(
			PedidoRepository pedidoRepository,
			TransactionTemplate transactionTemplate,
			Clock clock,
			@Value("${app.simulacao-entrega.aceitar-apos}") Duration aceitarApos,
			@Value("${app.simulacao-entrega.preparar-apos}") Duration prepararApos,
			@Value("${app.simulacao-entrega.sair-apos}") Duration sairApos,
			@Value("${app.simulacao-entrega.entregar-apos}") Duration entregarApos
	) {
		this.pedidoRepository = pedidoRepository;
		this.transactionTemplate = transactionTemplate;
		this.clock = clock;
		this.passos = List.of(
				new Passo(StatusPedido.PAGO, StatusPedido.ACEITO, aceitarApos),
				new Passo(StatusPedido.ACEITO, StatusPedido.EM_PREPARO, prepararApos),
				new Passo(StatusPedido.EM_PREPARO, StatusPedido.SAIU_PARA_ENTREGA, sairApos),
				new Passo(StatusPedido.SAIU_PARA_ENTREGA, StatusPedido.ENTREGUE, entregarApos));
	}

	@Scheduled(fixedDelayString = "${app.simulacao-entrega.intervalo-ms}")
	public void avancarPedidosParados() {
		for (Passo passo : passos) {
			transactionTemplate.executeWithoutResult(status -> avancar(passo));
		}
	}

	private void avancar(Passo passo) {
		Instant agora = Instant.now(clock);
		List<Pedido> parados = pedidoRepository.travarParadosDesde(passo.de().name(), agora.minus(passo.espera()));

		for (Pedido pedido : parados) {
			pedido.mudarStatus(passo.para(), agora);
			pedidoRepository.save(pedido);
			log.info("Simulação: pedido {} passou de {} para {}", pedido.getId(), passo.de(), passo.para());
		}
	}
}
