package com.pedeja.gatewayfake;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedeja.pagamento.webhook.AssinaturaWebhook;
import com.pedeja.pagamento.webhook.EventoPagamento;
import com.pedeja.pagamento.webhook.EventoPagamento.Resultado;

import jakarta.annotation.PreDestroy;

/**
 * Entrega as notificações do gateway falso imitando o que os gateways reais
 * fazem: chamada assíncrona, nova tentativa com espera crescente quando a API
 * não responde 2xx, e às vezes a mesma notificação entregue duas vezes. A API
 * precisa aguentar tudo isso.
 */
@Component
@ConditionalOnProperty(name = "app.gateway-fake.enabled", havingValue = "true")
public class EntregadorWebhook {

	private static final Logger log = LoggerFactory.getLogger(EntregadorWebhook.class);
	private static final int MAXIMO_TENTATIVAS = 5;

	private final ScheduledExecutorService agendador = Executors.newSingleThreadScheduledExecutor(tarefa -> {
		Thread thread = new Thread(tarefa, "gateway-fake-webhook");
		thread.setDaemon(true);
		return thread;
	});

	private final RestClient restClient = RestClient.create();
	private final AssinaturaWebhook assinatura;
	private final ObjectMapper objectMapper;
	private final Clock clock;
	private final long atrasoMs;
	private final boolean entregasDuplicadas;
	private String urlWebhook;

	public EntregadorWebhook(
			AssinaturaWebhook assinatura,
			ObjectMapper objectMapper,
			Clock clock,
			@Value("${app.gateway-fake.webhook-url:}") String urlWebhook,
			@Value("${app.gateway-fake.atraso-ms}") long atrasoMs,
			@Value("${app.gateway-fake.entregas-duplicadas}") boolean entregasDuplicadas
	) {
		this.assinatura = assinatura;
		this.objectMapper = objectMapper;
		this.clock = clock;
		this.urlWebhook = urlWebhook.isBlank() ? null : urlWebhook;
		this.atrasoMs = atrasoMs;
		this.entregasDuplicadas = entregasDuplicadas;
	}

	/** Sem URL configurada, o webhook é a própria aplicação, na porta em que ela subiu. */
	@EventListener
	public void aoSubirServidor(WebServerInitializedEvent evento) {
		if (urlWebhook == null) {
			urlWebhook = "http://localhost:%d/api/v1/webhooks/pagamentos".formatted(evento.getWebServer().getPort());
		}
	}

	public void agendar(String cobrancaId, Resultado resultado) {
		var evento = new EventoPagamento("evt_" + UUID.randomUUID(), cobrancaId, resultado, Instant.now(clock));
		agendador.schedule(() -> entregar(evento, 1), atrasoMs, TimeUnit.MILLISECONDS);

		if (entregasDuplicadas) {
			agendador.schedule(() -> entregar(evento, 1), atrasoMs + 300, TimeUnit.MILLISECONDS);
		}
	}

	private void entregar(EventoPagamento evento, int tentativa) {
		if (urlWebhook == null) {
			log.warn("Sem URL de webhook; evento {} não entregue", evento.eventoId());
			return;
		}

		try {
			// Assina na hora do envio: cada tentativa leva um timestamp novo.
			String corpo = objectMapper.writeValueAsString(evento);
			restClient.post()
					.uri(urlWebhook)
					.contentType(MediaType.APPLICATION_JSON)
					.header(AssinaturaWebhook.HEADER, assinatura.assinar(corpo))
					.body(corpo)
					.retrieve()
					.toBodilessEntity();
			log.info("Evento {} entregue na tentativa {}", evento.eventoId(), tentativa);
		} catch (JsonProcessingException e) {
			log.error("Evento {} não pôde ser serializado", evento.eventoId(), e);
		} catch (RuntimeException e) {
			if (tentativa >= MAXIMO_TENTATIVAS) {
				log.error("Evento {} desistido após {} tentativas", evento.eventoId(), tentativa, e);
				return;
			}
			long espera = atrasoMs * (1L << tentativa);
			log.warn("Evento {} falhou na tentativa {} ({}); nova tentativa em {} ms",
					evento.eventoId(), tentativa, e.getMessage(), espera);
			agendador.schedule(() -> entregar(evento, tentativa + 1), espera, TimeUnit.MILLISECONDS);
		}
	}

	@PreDestroy
	void encerrar() {
		agendador.shutdownNow();
	}
}
