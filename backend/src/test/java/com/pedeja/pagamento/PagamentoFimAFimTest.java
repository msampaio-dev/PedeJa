package com.pedeja.pagamento;

import static com.pedeja.shared.Cenario.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedeja.shared.Cenario;
import com.pedeja.shared.ContainersIntegrationTest;

/**
 * Sobe o servidor numa porta de verdade: o gateway falso entrega o webhook por
 * HTTP, de forma assíncrona e em dobro, como faria um gateway real.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class PagamentoFimAFimTest extends ContainersIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void pagamentoSimuladoNoSandboxChegaPeloWebhookEPagaOPedidoUmaVezSo() throws Exception {
		Cenario cenario = new Cenario(mockMvc, objectMapper);
		String cliente = cenario.novoCliente();
		long pedido = cenario.novoPedido(cliente, cenario.restauranteAberto("25.00"));

		String cobranca = cenario.json(mockMvc.perform(post("/api/v1/pedidos/{id}/pagamento", pedido)
				.header("Authorization", bearer(cliente))))
				.get("cobrancaId").asText();

		mockMvc.perform(post("/api/v1/gateway-fake/cobrancas/{id}/simulacao", cobranca)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"resultado\":\"APROVADO\"}"))
				.andExpect(status().isAccepted());

		await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
			JsonNode atual = cenario.json(mockMvc.perform(get("/api/v1/pedidos/{id}", pedido)
					.header("Authorization", bearer(cliente))));
			assertThat(atual.get("status").asText()).isEqualTo("PAGO");
		});

		// O gateway falso manda cada notificação duas vezes. Espera a segunda
		// chegar e confere que ela não gerou um segundo PAGO no histórico.
		Thread.sleep(800);
		Integer pagos = jdbcTemplate.queryForObject(
				"SELECT count(*) FROM historico_status_pedido WHERE pedido_id = ? AND status = 'PAGO'",
				Integer.class, pedido);
		assertThat(pagos).isEqualTo(1);
	}

	@Test
	void sandboxNaoAceitaPagarDuasVezesAMesmaCobranca() throws Exception {
		Cenario cenario = new Cenario(mockMvc, objectMapper);
		String cliente = cenario.novoCliente();
		long pedido = cenario.novoPedido(cliente, cenario.restauranteAberto("25.00"));
		String cobranca = cenario.json(mockMvc.perform(post("/api/v1/pedidos/{id}/pagamento", pedido)
				.header("Authorization", bearer(cliente))))
				.get("cobrancaId").asText();

		mockMvc.perform(post("/api/v1/gateway-fake/cobrancas/{id}/simulacao", cobranca)
				.contentType(MediaType.APPLICATION_JSON).content("{\"resultado\":\"RECUSADO\"}"))
				.andExpect(status().isAccepted());
		mockMvc.perform(post("/api/v1/gateway-fake/cobrancas/{id}/simulacao", cobranca)
				.contentType(MediaType.APPLICATION_JSON).content("{\"resultado\":\"APROVADO\"}"))
				.andExpect(status().isConflict());
	}
}
