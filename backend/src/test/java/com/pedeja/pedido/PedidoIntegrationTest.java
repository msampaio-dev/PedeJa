package com.pedeja.pedido;

import static com.pedeja.shared.Cenario.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedeja.shared.Cenario;
import com.pedeja.shared.Cenario.RestauranteDeTeste;
import com.pedeja.shared.ContainersIntegrationTest;

@SpringBootTest
@AutoConfigureMockMvc
class PedidoIntegrationTest extends ContainersIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Cenario cenario;

	@BeforeEach
	void criarCenario() {
		cenario = new Cenario(mockMvc, objectMapper);
	}

	@Test
	void calculaOTotalComOsPrecosDoBancoEGuardaUmaCopia() throws Exception {
		String cliente = cenario.novoCliente();
		RestauranteDeTeste restaurante = cenario.restauranteAberto("29.90", "16.00");
		String itens = "[{\"itemId\":%d,\"quantidade\":2},{\"itemId\":%d,\"quantidade\":1}]"
				.formatted(restaurante.itens().get(0), restaurante.itens().get(1));

		long pedidoId = cenario.json(cenario.fazerPedido(cliente, restaurante.id(), itens)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"))
				.andExpect(jsonPath("$.subtotal").value(75.80))
				.andExpect(jsonPath("$.taxaEntrega").value(5.00))
				.andExpect(jsonPath("$.total").value(80.80)))
				.get("id").asLong();

		// O restaurante sobe o preço depois do pedido.
		mockMvc.perform(put("/api/v1/meu-restaurante/itens/{id}", restaurante.itens().get(0))
				.header("Authorization", bearer(restaurante.token()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Item 0\",\"preco\":99.00,\"disponivel\":true}"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/pedidos/{id}", pedidoId).header("Authorization", bearer(cliente)))
				.andExpect(jsonPath("$.itens[0].precoUnitario").value(29.90))
				.andExpect(jsonPath("$.total").value(80.80));
	}

	@Test
	void juntaOMesmoItemRepetidoNumaLinhaSo() throws Exception {
		String cliente = cenario.novoCliente();
		RestauranteDeTeste restaurante = cenario.restauranteAberto("10.00");
		long item = restaurante.itens().get(0);

		cenario.fazerPedido(cliente, restaurante.id(),
				"[{\"itemId\":%d,\"quantidade\":1},{\"itemId\":%d,\"quantidade\":2}]".formatted(item, item))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.itens.length()").value(1))
				.andExpect(jsonPath("$.itens[0].quantidade").value(3))
				.andExpect(jsonPath("$.subtotal").value(30.00));
	}

	@Test
	void recusaItemDeOutroRestaurante() throws Exception {
		String cliente = cenario.novoCliente();
		RestauranteDeTeste restaurante = cenario.restauranteAberto("10.00");
		RestauranteDeTeste outro = cenario.restauranteAberto("12.00");

		cenario.fazerPedido(cliente, restaurante.id(), "[{\"itemId\":%d,\"quantidade\":1}]".formatted(outro.itens().get(0)))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.mensagem").value("Um dos itens não pertence ao cardápio deste restaurante"));
	}

	@Test
	void recusaItemIndisponivelDizendoQualE() throws Exception {
		String cliente = cenario.novoCliente();
		RestauranteDeTeste restaurante = cenario.restauranteAberto("10.00");
		long esgotado = cenario.criarItem(restaurante.token(), "Pudim", "9.00", false);

		cenario.fazerPedido(cliente, restaurante.id(), "[{\"itemId\":%d,\"quantidade\":1}]".formatted(esgotado))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.mensagem").value("O item \"Pudim\" não está mais disponível"));
	}

	@Test
	void recusaPedidoParaRestauranteFechado() throws Exception {
		String cliente = cenario.novoCliente();
		RestauranteDeTeste restaurante = cenario.restauranteAberto("10.00");
		mockMvc.perform(patch("/api/v1/meu-restaurante/abertura").header("Authorization", bearer(restaurante.token()))
				.contentType(MediaType.APPLICATION_JSON).content("{\"aberto\":false}"));

		cenario.fazerPedido(cliente, restaurante.id(), "[{\"itemId\":%d,\"quantidade\":1}]".formatted(restaurante.itens().get(0)))
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void mesmaChaveDeIdempotenciaDevolveOMesmoPedido() throws Exception {
		String cliente = cenario.novoCliente();
		RestauranteDeTeste restaurante = cenario.restauranteAberto("10.00");
		String chave = UUID.randomUUID().toString();

		long primeiro = cenario.json(pedidoComChave(cliente, restaurante, chave).andExpect(status().isCreated())).get("id").asLong();
		long repetido = cenario.json(pedidoComChave(cliente, restaurante, chave).andExpect(status().isOk())).get("id").asLong();

		assertThat(repetido).isEqualTo(primeiro);
		mockMvc.perform(get("/api/v1/pedidos").header("Authorization", bearer(cliente)))
				.andExpect(jsonPath("$.totalElementos").value(1));
	}

	@Test
	void clienteNaoVeNemCancelaPedidoDeOutroCliente() throws Exception {
		RestauranteDeTeste restaurante = cenario.restauranteAberto("10.00");
		long pedidoDoOutro = cenario.novoPedido(cenario.novoCliente(), restaurante);
		String intruso = cenario.novoCliente();

		mockMvc.perform(get("/api/v1/pedidos/{id}", pedidoDoOutro).header("Authorization", bearer(intruso)))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/v1/pedidos/{id}/cancelamento", pedidoDoOutro).header("Authorization", bearer(intruso)))
				.andExpect(status().isNotFound());
	}

	@Test
	void clienteCancelaPedidoNaoPagoUmaVezSo() throws Exception {
		String cliente = cenario.novoCliente();
		long pedido = cenario.novoPedido(cliente, cenario.restauranteAberto("10.00"));

		mockMvc.perform(post("/api/v1/pedidos/{id}/cancelamento", pedido).header("Authorization", bearer(cliente)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELADO"))
				.andExpect(jsonPath("$.historico.length()").value(2));

		mockMvc.perform(post("/api/v1/pedidos/{id}/cancelamento", pedido).header("Authorization", bearer(cliente)))
				.andExpect(status().isConflict());
	}

	@Test
	void restauranteNaoEnxergaPedidoAntesDoPagamento() throws Exception {
		RestauranteDeTeste restaurante = cenario.restauranteAberto("10.00");
		long pedido = cenario.novoPedido(cenario.novoCliente(), restaurante);

		mockMvc.perform(get("/api/v1/meu-restaurante/pedidos").header("Authorization", bearer(restaurante.token())))
				.andExpect(jsonPath("$.length()").value(0));
		mudarStatus(restaurante.token(), pedido, "ACEITO").andExpect(status().isNotFound());
	}

	@Test
	void restauranteSegueAMaquinaDeEstadosDepoisDoPagamento() throws Exception {
		RestauranteDeTeste restaurante = cenario.restauranteAberto("10.00");
		long pedido = cenario.novoPedido(cenario.novoCliente(), restaurante);
		marcarComoPago(pedido);

		mudarStatus(restaurante.token(), pedido, "EM_PREPARO").andExpect(status().isConflict());
		mudarStatus(restaurante.token(), pedido, "ACEITO").andExpect(status().isOk());
		mudarStatus(restaurante.token(), pedido, "EM_PREPARO").andExpect(status().isOk());
		mudarStatus(restaurante.token(), pedido, "SAIU_PARA_ENTREGA").andExpect(status().isOk());
		mudarStatus(restaurante.token(), pedido, "ENTREGUE")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.historico.length()").value(6));
	}

	@Test
	void restauranteNaoMarcaComoPagoNemCancela() throws Exception {
		RestauranteDeTeste restaurante = cenario.restauranteAberto("10.00");
		long pedido = cenario.novoPedido(cenario.novoCliente(), restaurante);
		marcarComoPago(pedido);

		mudarStatus(restaurante.token(), pedido, "CANCELADO").andExpect(status().isUnprocessableEntity());
		mudarStatus(restaurante.token(), pedido, "PAGO").andExpect(status().isUnprocessableEntity());
	}

	@Test
	void restauranteNaoUsaRotasDeCliente() throws Exception {
		RestauranteDeTeste restaurante = cenario.restauranteAberto("10.00");

		mockMvc.perform(get("/api/v1/pedidos").header("Authorization", bearer(restaurante.token())))
				.andExpect(status().isForbidden());
	}

	private ResultActions pedidoComChave(String cliente, RestauranteDeTeste restaurante, String chave) throws Exception {
		return mockMvc.perform(post("/api/v1/pedidos")
				.header("Authorization", bearer(cliente))
				.header("Idempotency-Key", chave)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"restauranteId\":%d,\"itens\":[{\"itemId\":%d,\"quantidade\":1}],\"enderecoEntrega\":\"Rua A, 10\"}"
						.formatted(restaurante.id(), restaurante.itens().get(0))));
	}

	private ResultActions mudarStatus(String token, long pedido, String status) throws Exception {
		return mockMvc.perform(patch("/api/v1/meu-restaurante/pedidos/{id}/status", pedido)
				.header("Authorization", bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"%s\"}".formatted(status)));
	}

	/** Atalho até a fase 4: o pagamento é o único caminho para PAGO pela API. */
	private void marcarComoPago(long pedido) {
		jdbcTemplate.update("UPDATE pedidos SET status = 'PAGO' WHERE id = ?", pedido);
		jdbcTemplate.update("INSERT INTO historico_status_pedido (pedido_id, status, ocorrido_em) VALUES (?, 'PAGO', now())", pedido);
	}
}
