package com.pedeja.shared;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Monta pelos endpoints públicos o que os testes de pedido e pagamento precisam:
 * cliente, restaurante aberto com cardápio e pedidos.
 */
public class Cenario {

	private final MockMvc mockMvc;
	private final ObjectMapper objectMapper;

	public Cenario(MockMvc mockMvc, ObjectMapper objectMapper) {
		this.mockMvc = mockMvc;
		this.objectMapper = objectMapper;
	}

	public record RestauranteDeTeste(String token, long id, List<Long> itens) {
	}

	public String novoCliente() throws Exception {
		String email = "cliente-" + UUID.randomUUID() + "@teste.com";
		mockMvc.perform(post("/api/v1/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Cliente\",\"email\":\"%s\",\"senha\":\"segredo123\"}".formatted(email)))
				.andExpect(status().isCreated());
		return login(email);
	}

	/** Restaurante aberto, taxa de entrega 5,00 e um item disponível por preço informado. */
	public RestauranteDeTeste restauranteAberto(String... precos) throws Exception {
		String email = "dono-" + UUID.randomUUID() + "@teste.com";
		mockMvc.perform(post("/api/v1/restaurantes/cadastro").contentType(MediaType.APPLICATION_JSON).content("""
				{"nomeResponsavel":"Dono","email":"%s","senha":"segredo123",
				 "restaurante":{"nome":"Restaurante %s","categoria":"LANCHES","taxaEntrega":5.00,"tempoEntregaMinutos":30}}
				""".formatted(email, UUID.randomUUID())))
				.andExpect(status().isCreated());
		String token = login(email);

		List<Long> itens = new ArrayList<>();
		for (int i = 0; i < precos.length; i++) {
			itens.add(criarItem(token, "Item " + i, precos[i], true));
		}

		mockMvc.perform(patch("/api/v1/meu-restaurante/abertura").header("Authorization", bearer(token))
				.contentType(MediaType.APPLICATION_JSON).content("{\"aberto\":true}"))
				.andExpect(status().isOk());

		JsonNode restaurante = json(mockMvc.perform(get("/api/v1/meu-restaurante").header("Authorization", bearer(token))));
		return new RestauranteDeTeste(token, restaurante.get("id").asLong(), itens);
	}

	public long criarItem(String tokenRestaurante, String nome, String preco, boolean disponivel) throws Exception {
		JsonNode item = json(mockMvc.perform(post("/api/v1/meu-restaurante/itens")
				.header("Authorization", bearer(tokenRestaurante))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"%s\",\"preco\":%s,\"disponivel\":%s}".formatted(nome, preco, disponivel)))
				.andExpect(status().isCreated()));
		return item.get("id").asLong();
	}

	public ResultActions fazerPedido(String tokenCliente, long restauranteId, String itensJson) throws Exception {
		return mockMvc.perform(post("/api/v1/pedidos")
				.header("Authorization", bearer(tokenCliente))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"restauranteId\":%d,\"itens\":%s,\"enderecoEntrega\":\"Rua A, 10\"}"
						.formatted(restauranteId, itensJson)));
	}

	public long novoPedido(String tokenCliente, RestauranteDeTeste restaurante) throws Exception {
		String itens = "[{\"itemId\":%d,\"quantidade\":1}]".formatted(restaurante.itens().get(0));
		return json(fazerPedido(tokenCliente, restaurante.id(), itens).andExpect(status().isCreated())).get("id").asLong();
	}

	public String login(String email) throws Exception {
		return json(mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"senha\":\"segredo123\"}".formatted(email)))
				.andExpect(status().isOk()))
				.get("token").asText();
	}

	public JsonNode json(ResultActions resultado) throws Exception {
		return objectMapper.readTree(resultado.andReturn().getResponse().getContentAsString());
	}

	public static String bearer(String token) {
		return "Bearer " + token;
	}
}
