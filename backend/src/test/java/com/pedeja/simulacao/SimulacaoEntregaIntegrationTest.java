package com.pedeja.simulacao;

import static com.pedeja.shared.Cenario.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedeja.shared.Cenario;
import com.pedeja.shared.Cenario.RestauranteDeTeste;
import com.pedeja.shared.ContainersIntegrationTest;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
		"app.simulacao-entrega.enabled=true",
		"app.simulacao-entrega.aceitar-apos=0s",
		"app.simulacao-entrega.preparar-apos=0s",
		"app.simulacao-entrega.sair-apos=0s",
		"app.simulacao-entrega.entregar-apos=0s",
		"app.simulacao-entrega.intervalo-ms=200"
})
class SimulacaoEntregaIntegrationTest extends ContainersIntegrationTest {

	private static final Duration ESPERA = Duration.ofSeconds(15);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void pedidoPagoAvancaSozinhoAteEntregueComTodasAsEtapasNoHistorico() throws Exception {
		Cenario cenario = new Cenario(mockMvc, objectMapper);
		String cliente = cenario.novoCliente();
		long pedido = cenario.novoPedido(cliente, cenario.restauranteAberto("20.00"));
		marcarComoPago(pedido);

		await().atMost(ESPERA).until(() -> status(pedido).equals("ENTREGUE"));

		List<String> historico = jdbcTemplate.queryForList(
				"SELECT status FROM historico_status_pedido WHERE pedido_id = ? ORDER BY id", String.class, pedido);
		assertThat(historico).containsExactly(
				"AGUARDANDO_PAGAMENTO", "PAGO", "ACEITO", "EM_PREPARO", "SAIU_PARA_ENTREGA", "ENTREGUE");
	}

	@Test
	void pedidoNaoPagoNaoAvanca() throws Exception {
		Cenario cenario = new Cenario(mockMvc, objectMapper);
		long pedido = cenario.novoPedido(cenario.novoCliente(), cenario.restauranteAberto("20.00"));

		Thread.sleep(1500);

		assertThat(status(pedido)).isEqualTo("AGUARDANDO_PAGAMENTO");
	}

	@Test
	void pedidoRecusadoPeloRestauranteNaoEhRetomado() throws Exception {
		Cenario cenario = new Cenario(mockMvc, objectMapper);
		RestauranteDeTeste restaurante = cenario.restauranteAberto("20.00");
		long pedido = cenario.novoPedido(cenario.novoCliente(), restaurante);
		// Pago "no futuro": o simulador ainda não o considera parado, e o restaurante recusa antes.
		jdbcTemplate.update("UPDATE pedidos SET status = 'PAGO', atualizado_em = now() + interval '1 hour' WHERE id = ?", pedido);

		mockMvc.perform(patch("/api/v1/meu-restaurante/pedidos/{id}/status", pedido)
				.header("Authorization", bearer(restaurante.token()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"RECUSADO\"}"));

		Thread.sleep(1500);
		assertThat(status(pedido)).isEqualTo("RECUSADO");
	}

	private void marcarComoPago(long pedido) {
		jdbcTemplate.update("UPDATE pedidos SET status = 'PAGO', atualizado_em = now() WHERE id = ?", pedido);
		jdbcTemplate.update("INSERT INTO historico_status_pedido (pedido_id, status, ocorrido_em) VALUES (?, 'PAGO', now())", pedido);
	}

	private String status(long pedido) {
		return jdbcTemplate.queryForObject("SELECT status FROM pedidos WHERE id = ?", String.class, pedido);
	}
}
