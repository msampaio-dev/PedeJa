package com.pedeja.mensageria;

import static com.pedeja.shared.Cenario.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedeja.pagamento.webhook.AssinaturaWebhook;
import com.pedeja.pedido.entity.StatusPedido;
import com.pedeja.shared.Cenario;
import com.pedeja.shared.Cenario.RestauranteDeTeste;
import com.pedeja.shared.ContainersIntegrationTest;

@SpringBootTest
@AutoConfigureMockMvc
class MensageriaIntegrationTest extends ContainersIntegrationTest {

	private static final Duration ESPERA = Duration.ofSeconds(15);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private TopologiaRabbit.Nomes nomes;

	@Autowired
	private AssinaturaWebhook assinatura;

	private Cenario cenario;

	@BeforeEach
	void criarCenario() {
		cenario = new Cenario(mockMvc, objectMapper);
	}

	@Test
	void pedidoNovoGravaEventoNoOutboxEOClienteRecebeNotificacao() throws Exception {
		String cliente = cenario.novoCliente();
		long pedido = cenario.novoPedido(cliente, cenario.restauranteAberto("20.00"));

		assertThat(eventosNoOutbox(pedido)).isEqualTo(1);

		await().atMost(ESPERA).untilAsserted(() ->
				mockMvc.perform(get("/api/v1/notificacoes").header("Authorization", bearer(cliente)))
						.andExpect(jsonPath("$[0].pedidoId").value(pedido))
						.andExpect(jsonPath("$[0].mensagem").value("Recebemos o pedido #%d. Falta só o pagamento.".formatted(pedido))));

		await().atMost(ESPERA).until(() -> jdbcTemplate.queryForObject(
				"SELECT count(*) FROM outbox_eventos WHERE pedido_id = ? AND publicado_em IS NOT NULL", Integer.class, pedido) == 1);
	}

	@Test
	void pagamentoAprovadoAvisaOClienteEODonoDoRestaurante() throws Exception {
		String cliente = cenario.novoCliente();
		RestauranteDeTeste restaurante = cenario.restauranteAberto("20.00");
		long pedido = cenario.novoPedido(cliente, restaurante);
		aprovarPagamento(cliente, pedido);

		await().atMost(ESPERA).untilAsserted(() ->
				mockMvc.perform(get("/api/v1/notificacoes").header("Authorization", bearer(restaurante.token())))
						.andExpect(jsonPath("$[0].mensagem").value("Novo pedido #%d pago. Aceite ou recuse.".formatted(pedido))));
	}

	@Test
	void falhaNaTransacaoNaoDeixaEventoNoOutbox() throws Exception {
		String cliente = cenario.novoCliente();
		long pedido = cenario.novoPedido(cliente, cenario.restauranteAberto("20.00"));
		mockMvc.perform(post("/api/v1/pedidos/{id}/pagamento", pedido).header("Authorization", bearer(cliente)));

		// Cancelar com Pix em aberto é recusado depois de o pedido ser travado. A
		// transação volta inteira e nenhum evento CANCELADO é gravado.
		mockMvc.perform(post("/api/v1/pedidos/{id}/cancelamento", pedido).header("Authorization", bearer(cliente)))
				.andExpect(status().isUnprocessableEntity());

		assertThat(eventosNoOutbox(pedido)).isEqualTo(1);
	}

	@Test
	void mensagemRepetidaNaoDuplicaANotificacao() throws Exception {
		String cliente = cenario.novoCliente();
		long pedido = cenario.novoPedido(cliente, cenario.restauranteAberto("20.00"));
		await().atMost(ESPERA).until(() -> notificacoes(pedido) == 1);

		// Reenvia a mesma mensagem, com o mesmo eventoId, como o outbox faria se a
		// API caísse entre o ack do RabbitMQ e a marcação de publicado.
		String payload = jdbcTemplate.queryForObject(
				"SELECT payload::text FROM outbox_eventos WHERE pedido_id = ?", String.class, pedido);
		rabbitTemplate.send(nomes.exchangePedidos(), "pedido.status.aguardando_pagamento", json(payload));

		Thread.sleep(1000);
		assertThat(notificacoes(pedido)).isEqualTo(1);
	}

	@Test
	void mensagemIlegivelVaiParaADlq() {
		String lixo = "{isto não é json " + UUID.randomUUID();
		rabbitTemplate.send(nomes.exchangePedidos(), "pedido.status.pago", json(lixo));

		await().atMost(ESPERA).untilAsserted(() -> {
			Message morta = rabbitTemplate.receive(nomes.filaNotificacoesMortas(), 500);
			assertThat(morta).isNotNull();
			assertThat(new String(morta.getBody(), StandardCharsets.UTF_8)).isEqualTo(lixo);
		});
	}

	@Test
	void routingKeyTrazOStatusEmMinusculas() {
		MensagemPedido mensagem = new MensagemPedido(
				UUID.randomUUID(), 1L, 2L, 3L, 4L, StatusPedido.SAIU_PARA_ENTREGA, Instant.now());

		assertThat(mensagem.routingKey()).isEqualTo("pedido.status.saiu_para_entrega");
	}

	private void aprovarPagamento(String cliente, long pedido) throws Exception {
		String cobranca = cenario.json(mockMvc.perform(post("/api/v1/pedidos/{id}/pagamento", pedido)
				.header("Authorization", bearer(cliente)))).get("cobrancaId").asText();
		String corpo = "{\"eventoId\":\"evt_%s\",\"cobrancaId\":\"%s\",\"resultado\":\"APROVADO\",\"ocorridoEm\":\"%s\"}"
				.formatted(UUID.randomUUID(), cobranca, Instant.now());
		mockMvc.perform(post("/api/v1/webhooks/pagamentos")
				.contentType(MediaType.APPLICATION_JSON)
				.header(AssinaturaWebhook.HEADER, assinatura.assinar(corpo))
				.content(corpo))
				.andExpect(status().isOk());
	}

	private long eventosNoOutbox(long pedido) {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM outbox_eventos WHERE pedido_id = ?", Long.class, pedido);
	}

	private int notificacoes(long pedido) {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM notificacoes WHERE pedido_id = ?", Integer.class, pedido);
	}

	private Message json(String corpo) {
		return MessageBuilder.withBody(corpo.getBytes(StandardCharsets.UTF_8)).setContentType("application/json").build();
	}
}
