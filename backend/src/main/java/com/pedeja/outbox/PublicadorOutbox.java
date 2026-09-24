package com.pedeja.outbox;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.pedeja.mensageria.TopologiaRabbit;

/**
 * Lê o outbox e publica no RabbitMQ o que ainda não foi publicado.
 *
 * A garantia é "pelo menos uma vez": se a API cair depois de o RabbitMQ
 * confirmar e antes de marcar publicado_em, o evento sai de novo na próxima
 * rodada. Por isso os consumidores descartam mensagens repetidas pelo eventoId.
 *
 * {@code @Lazy(false)}: em produção a inicialização é preguiçosa para caber na
 * CPU do plano gratuito, e um bean que ninguém pede nunca seria criado, nem o
 * agendamento dele.
 */
@Component
@Lazy(false)
public class PublicadorOutbox {

	private static final Logger log = LoggerFactory.getLogger(PublicadorOutbox.class);
	private static final int TAMANHO_LOTE = 50;
	private static final long ESPERA_CONFIRMACAO_MS = 5000;

	private final OutboxRepository outboxRepository;
	private final RabbitTemplate rabbitTemplate;
	private final TransactionTemplate transactionTemplate;
	private final Clock clock;
	private final String exchange;

	public PublicadorOutbox(
			OutboxRepository outboxRepository,
			RabbitTemplate rabbitTemplate,
			TransactionTemplate transactionTemplate,
			Clock clock,
			TopologiaRabbit.Nomes nomes
	) {
		this.outboxRepository = outboxRepository;
		this.rabbitTemplate = rabbitTemplate;
		this.transactionTemplate = transactionTemplate;
		this.clock = clock;
		this.exchange = nomes.exchangePedidos();
	}

	@Scheduled(fixedDelayString = "${app.outbox.intervalo-ms}")
	public void publicarPendentes() {
		transactionTemplate.executeWithoutResult(status -> {
			List<OutboxEvento> pendentes = outboxRepository.travarPendentes(TAMANHO_LOTE);

			for (OutboxEvento evento : pendentes) {
				try {
					publicarComConfirmacao(evento);
					evento.marcarPublicado(Instant.now(clock));
				} catch (RuntimeException e) {
					// Broker fora do ar: o resto do lote falharia igual. Registra e para;
					// a próxima rodada tenta de novo a partir deste evento.
					evento.registrarFalha(e.getMessage());
					log.warn("Falha ao publicar o evento {}: {}", evento.getId(), e.getMessage());
					break;
				}
			}
		});
	}

	/**
	 * Publica e espera o "ack" do RabbitMQ (publisher confirm). Sem isso, a
	 * mensagem poderia se perder no caminho e o outbox a marcaria como publicada.
	 */
	private void publicarComConfirmacao(OutboxEvento evento) {
		Message mensagem = MessageBuilder.withBody(evento.getPayload().getBytes(StandardCharsets.UTF_8))
				.setContentType(MessageProperties.CONTENT_TYPE_JSON)
				.setContentEncoding(StandardCharsets.UTF_8.name())
				.setMessageId(evento.getId().toString())
				.setDeliveryMode(MessageDeliveryMode.PERSISTENT)
				.build();

		rabbitTemplate.invoke(operacoes -> {
			operacoes.send(exchange, evento.getRoutingKey(), mensagem);
			operacoes.waitForConfirmsOrDie(ESPERA_CONFIRMACAO_MS);
			return null;
		});
	}
}
