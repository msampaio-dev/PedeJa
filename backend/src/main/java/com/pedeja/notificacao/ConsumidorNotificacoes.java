package com.pedeja.notificacao;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedeja.mensageria.MensagemPedido;

/**
 * Faz o papel do serviço de e-mail ou push: cada mudança de status vira uma
 * notificação gravada para o cliente (e para o restaurante, quando é pedido pago).
 *
 * Falha inesperada volta para a fila e é tentada de novo (3 vezes, com espera
 * crescente, configurado em application.properties). Esgotadas as tentativas,
 * a mensagem vai para a DLQ, onde dá para inspecionar e reprocessar à mão.
 */
@Component
@Lazy(false)
public class ConsumidorNotificacoes {

	private static final Logger log = LoggerFactory.getLogger(ConsumidorNotificacoes.class);

	private final NotificacaoRepository notificacaoRepository;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	public ConsumidorNotificacoes(NotificacaoRepository notificacaoRepository, ObjectMapper objectMapper, Clock clock) {
		this.notificacaoRepository = notificacaoRepository;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	@RabbitListener(queues = "#{@filaNotificacoes.name}")
	@Transactional
	public void receber(Message mensagem) {
		MensagemPedido evento = ler(mensagem);
		Instant agora = Instant.now(clock);

		for (TextoNotificacao.Destino destino : TextoNotificacao.para(evento)) {
			notificacaoRepository.inserirSeNova(evento.eventoId(), destino.usuarioId(), evento.pedidoId(), destino.mensagem(), agora);
		}
	}

	/**
	 * JSON quebrado não melhora com nova tentativa. A exceção "reject and don't
	 * requeue" garante que a mensagem termine na DLQ, e não de volta na fila.
	 */
	private MensagemPedido ler(Message mensagem) {
		try {
			return objectMapper.readValue(new String(mensagem.getBody(), StandardCharsets.UTF_8), MensagemPedido.class);
		} catch (JsonProcessingException e) {
			log.error("Mensagem ilegível na fila de notificações; enviada para a DLQ: {}", e.getOriginalMessage());
			throw new AmqpRejectAndDontRequeueException("Mensagem ilegível", e);
		}
	}
}
