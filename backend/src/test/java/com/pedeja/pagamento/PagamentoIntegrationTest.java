package com.pedeja.pagamento;

import static com.pedeja.shared.Cenario.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedeja.pagamento.webhook.AssinaturaWebhook;
import com.pedeja.shared.Cenario;
import com.pedeja.shared.ContainersIntegrationTest;

/**
 * Chama o webhook direto, como o gateway chamaria, com eventos assinados pelo
 * próprio teste. O caminho completo pelo gateway falso está em PagamentoFimAFimTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PagamentoIntegrationTest extends ContainersIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private AssinaturaWebhook assinatura;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Value("${app.pagamento.webhook-secret}")
	private String segredoWebhook;

	private Cenario cenario;
	private String cliente;
	private long pedido;

	@BeforeEach
	void criarPedido() throws Exception {
		cenario = new Cenario(mockMvc, objectMapper);
		cliente = cenario.novoCliente();
		pedido = cenario.novoPedido(cliente, cenario.restauranteAberto("25.00"));
	}

	@Test
	void geraCobrancaPendenteComOValorDoPedidoEDevolveAMesmaNaSegundaChamada() throws Exception {
		String primeira = iniciarPagamento()
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PENDENTE"))
				.andExpect(jsonPath("$.valor").value(30.00))
				.andExpect(jsonPath("$.pixCopiaECola").isNotEmpty())
				.andReturn().getResponse().getContentAsString();

		String segunda = iniciarPagamento().andReturn().getResponse().getContentAsString();

		assertThat(objectMapper.readTree(segunda).get("cobrancaId"))
				.isEqualTo(objectMapper.readTree(primeira).get("cobrancaId"));
	}

	@Test
	void webhookAprovadoMarcaOPedidoComoPago() throws Exception {
		String cobranca = cobrancaId(iniciarPagamento());

		webhook(evento("evt_" + UUID.randomUUID(), cobranca, "APROVADO")).andExpect(status().isOk());

		consultarPedido()
				.andExpect(jsonPath("$.status").value("PAGO"))
				.andExpect(jsonPath("$.historico[1].status").value("PAGO"));
		mockMvc.perform(get("/api/v1/pedidos/{id}/pagamento", pedido).header("Authorization", bearer(cliente)))
				.andExpect(jsonPath("$.status").value("APROVADO"));
	}

	@Test
	void mesmoEventoEntregueDuasVezesTemEfeitoUmaVezSo() throws Exception {
		String cobranca = cobrancaId(iniciarPagamento());
		String evento = evento("evt_" + UUID.randomUUID(), cobranca, "APROVADO");

		webhook(evento).andExpect(status().isOk());
		webhook(evento).andExpect(status().isOk());

		consultarPedido().andExpect(jsonPath("$.historico.length()").value(2));
	}

	@Test
	void recusaWebhookComAssinaturaErradaSemMexerNoPedido() throws Exception {
		String cobranca = cobrancaId(iniciarPagamento());
		String corpo = evento("evt_" + UUID.randomUUID(), cobranca, "APROVADO");
		long agora = Instant.now().getEpochSecond();

		mockMvc.perform(post("/api/v1/webhooks/pagamentos")
				.contentType(MediaType.APPLICATION_JSON)
				.header(AssinaturaWebhook.HEADER, "t=%d,v1=%s".formatted(agora, "0".repeat(64)))
				.content(corpo))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/v1/webhooks/pagamentos").contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isUnauthorized());

		consultarPedido().andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"));
	}

	@Test
	void recusaWebhookComCorpoAlteradoDepoisDeAssinado() throws Exception {
		String cobranca = cobrancaId(iniciarPagamento());
		String original = evento("evt_" + UUID.randomUUID(), cobranca, "RECUSADO");
		String adulterado = original.replace("RECUSADO", "APROVADO");

		mockMvc.perform(post("/api/v1/webhooks/pagamentos")
				.contentType(MediaType.APPLICATION_JSON)
				.header(AssinaturaWebhook.HEADER, assinatura.assinar(original))
				.content(adulterado))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void recusaWebhookAssinadoHaMaisDeCincoMinutos() throws Exception {
		String cobranca = cobrancaId(iniciarPagamento());
		String corpo = evento("evt_" + UUID.randomUUID(), cobranca, "APROVADO");
		// Assinatura correta, com o segredo certo, mas feita seis minutos atrás:
		// é o que um atacante teria ao capturar uma notificação e reenviá-la depois.
		Clock seisMinutosAtras = Clock.fixed(Instant.now().minusSeconds(360), ZoneOffset.UTC);
		String antiga = new AssinaturaWebhook(segredoWebhook, seisMinutosAtras).assinar(corpo);

		mockMvc.perform(post("/api/v1/webhooks/pagamentos")
				.contentType(MediaType.APPLICATION_JSON)
				.header(AssinaturaWebhook.HEADER, antiga)
				.content(corpo))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void pagamentoRecusadoDeixaOPedidoEsperandoEPermiteNovaCobranca() throws Exception {
		String primeira = cobrancaId(iniciarPagamento());

		webhook(evento("evt_" + UUID.randomUUID(), primeira, "RECUSADO")).andExpect(status().isOk());

		consultarPedido().andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"));
		String segunda = cobrancaId(iniciarPagamento().andExpect(jsonPath("$.status").value("PENDENTE")));
		assertThat(segunda).isNotEqualTo(primeira);
	}

	@Test
	void naoCancelaComCobrancaPixEmAberto() throws Exception {
		iniciarPagamento();

		mockMvc.perform(post("/api/v1/pedidos/{id}/cancelamento", pedido).header("Authorization", bearer(cliente)))
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void cancelaDepoisQueACobrancaVence() throws Exception {
		iniciarPagamento();
		jdbcTemplate.update("UPDATE pagamentos SET expira_em = now() - interval '1 minute' WHERE pedido_id = ?", pedido);

		mockMvc.perform(post("/api/v1/pedidos/{id}/cancelamento", pedido).header("Authorization", bearer(cliente)))
				.andExpect(status().isOk());
	}

	@Test
	void naoGeraCobrancaParaPedidoCancelado() throws Exception {
		mockMvc.perform(post("/api/v1/pedidos/{id}/cancelamento", pedido).header("Authorization", bearer(cliente)));

		iniciarPagamento().andExpect(status().isConflict());
	}

	@Test
	void outroClienteNaoPagaNemConsultaOPagamento() throws Exception {
		String intruso = cenario.novoCliente();

		mockMvc.perform(post("/api/v1/pedidos/{id}/pagamento", pedido).header("Authorization", bearer(intruso)))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/pedidos/{id}/pagamento", pedido).header("Authorization", bearer(intruso)))
				.andExpect(status().isNotFound());
	}

	private ResultActions iniciarPagamento() throws Exception {
		return mockMvc.perform(post("/api/v1/pedidos/{id}/pagamento", pedido).header("Authorization", bearer(cliente)));
	}

	private ResultActions consultarPedido() throws Exception {
		return mockMvc.perform(get("/api/v1/pedidos/{id}", pedido).header("Authorization", bearer(cliente)));
	}

	private String cobrancaId(ResultActions resultado) throws Exception {
		return cenario.json(resultado).get("cobrancaId").asText();
	}

	private String evento(String eventoId, String cobrancaId, String resultado) {
		return "{\"eventoId\":\"%s\",\"cobrancaId\":\"%s\",\"resultado\":\"%s\",\"ocorridoEm\":\"%s\"}"
				.formatted(eventoId, cobrancaId, resultado, Instant.now());
	}

	private ResultActions webhook(String corpo) throws Exception {
		return mockMvc.perform(post("/api/v1/webhooks/pagamentos")
				.contentType(MediaType.APPLICATION_JSON)
				.header(AssinaturaWebhook.HEADER, assinatura.assinar(corpo))
				.content(corpo));
	}
}
