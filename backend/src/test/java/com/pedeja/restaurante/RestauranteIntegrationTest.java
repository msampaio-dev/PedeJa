package com.pedeja.restaurante;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedeja.shared.PostgresIntegrationTest;

@SpringBootTest
@AutoConfigureMockMvc
class RestauranteIntegrationTest extends PostgresIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void cadastroCriaContaDeRestauranteQueComecaFechado() throws Exception {
		String email = emailUnico();
		cadastrarRestaurante(email, "Pizzaria Teste " + UUID.randomUUID(), "PIZZA")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.aberto").value(false));

		String token = login(email);

		mockMvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(token)))
				.andExpect(jsonPath("$.perfil").value("RESTAURANTE"));
	}

	@Test
	void cadastroComRestauranteInvalidoNaoDeixaContaCriada() throws Exception {
		String email = emailUnico();
		String corpo = """
				{"nomeResponsavel":"Dono","email":"%s","senha":"segredo123",
				 "restaurante":{"nome":"","categoria":"PIZZA","taxaEntrega":-1,"tempoEntregaMinutos":2}}
				""".formatted(email);

		mockMvc.perform(post("/api/v1/restaurantes/cadastro").contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos.length()").value(3));

		mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"senha\":\"segredo123\"}".formatted(email)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void naoAbreRestauranteSemItemDisponivel() throws Exception {
		String token = novoRestaurante();

		abrir(token, true)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.mensagem").value("Cadastre ao menos um item disponível antes de abrir o restaurante"));

		criarItem(token, "Pizza Margherita", "49.90", true).andExpect(status().isCreated());

		abrir(token, true)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.aberto").value(true));
	}

	@Test
	void cardapioPublicoEscondeItensIndisponiveis() throws Exception {
		String token = novoRestaurante();
		criarItem(token, "Pizza Margherita", "49.90", true);
		criarItem(token, "Tiramisù", "22.00", false);
		Long restauranteId = idDoRestaurante(token);

		mockMvc.perform(get("/api/v1/restaurantes/{id}/cardapio", restauranteId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.itens.length()").value(1))
				.andExpect(jsonPath("$.itens[0].nome").value("Pizza Margherita"))
				.andExpect(jsonPath("$.itens[0].preco").value(49.90));

		mockMvc.perform(get("/api/v1/meu-restaurante/itens").header("Authorization", bearer(token)))
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void recusaItemComNomeRepetidoMesmoComOutraCaixa() throws Exception {
		String token = novoRestaurante();
		criarItem(token, "Pizza Margherita", "49.90", true);

		criarItem(token, "pizza margherita", "39.90", true)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensagem").value("Já existe um item com esse nome no cardápio"));
	}

	@Test
	void recusaPrecoZeroOuComMaisDeDuasCasas() throws Exception {
		String token = novoRestaurante();

		criarItem(token, "Grátis", "0", true).andExpect(status().isBadRequest());
		criarItem(token, "Quebrado", "10.999", true).andExpect(status().isBadRequest());
	}

	@Test
	void donoNaoEditaItemDeOutroRestaurante() throws Exception {
		String tokenDonoA = novoRestaurante();
		String tokenDonoB = novoRestaurante();
		String resposta = criarItem(tokenDonoA, "Pizza Margherita", "49.90", true)
				.andReturn().getResponse().getContentAsString();
		long itemDoA = objectMapper.readTree(resposta).get("id").asLong();

		mockMvc.perform(put("/api/v1/meu-restaurante/itens/{id}", itemDoA)
				.header("Authorization", bearer(tokenDonoB))
				.contentType(MediaType.APPLICATION_JSON)
				.content(corpoItem("Invadido", "1.00", true)))
				.andExpect(status().isNotFound());
	}

	@Test
	void clienteNaoAcessaAreaDoRestaurante() throws Exception {
		String email = emailUnico();
		mockMvc.perform(post("/api/v1/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Cliente\",\"email\":\"%s\",\"senha\":\"segredo123\"}".formatted(email)))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/meu-restaurante").header("Authorization", bearer(login(email))))
				.andExpect(status().isForbidden());
	}

	@Test
	void listagemPublicaFiltraPorCategoriaENome() throws Exception {
		String sufixo = UUID.randomUUID().toString();
		cadastrarRestaurante(emailUnico(), "Sushi " + sufixo, "JAPONESA");
		cadastrarRestaurante(emailUnico(), "Burger " + sufixo, "LANCHES");

		mockMvc.perform(get("/api/v1/restaurantes").param("busca", sufixo.toUpperCase()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElementos").value(2));

		mockMvc.perform(get("/api/v1/restaurantes").param("busca", sufixo).param("categoria", "JAPONESA"))
				.andExpect(jsonPath("$.totalElementos").value(1))
				.andExpect(jsonPath("$.conteudo[0].nome").value("Sushi " + sufixo));
	}

	@Test
	void listagemRecusaTamanhoDePaginaAcimaDoLimite() throws Exception {
		mockMvc.perform(get("/api/v1/restaurantes").param("tamanho", "500"))
				.andExpect(status().isBadRequest());
	}

	private String novoRestaurante() throws Exception {
		String email = emailUnico();
		cadastrarRestaurante(email, "Restaurante " + UUID.randomUUID(), "PIZZA").andExpect(status().isCreated());
		return login(email);
	}

	private ResultActions cadastrarRestaurante(String email, String nome, String categoria) throws Exception {
		String corpo = """
				{"nomeResponsavel":"Dono","email":"%s","senha":"segredo123",
				 "restaurante":{"nome":"%s","categoria":"%s","taxaEntrega":5.00,"tempoEntregaMinutos":40}}
				""".formatted(email, nome, categoria);
		return mockMvc.perform(post("/api/v1/restaurantes/cadastro").contentType(MediaType.APPLICATION_JSON).content(corpo));
	}

	private ResultActions criarItem(String token, String nome, String preco, boolean disponivel) throws Exception {
		return mockMvc.perform(post("/api/v1/meu-restaurante/itens")
				.header("Authorization", bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content(corpoItem(nome, preco, disponivel)));
	}

	private ResultActions abrir(String token, boolean aberto) throws Exception {
		return mockMvc.perform(patch("/api/v1/meu-restaurante/abertura")
				.header("Authorization", bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"aberto\":%s}".formatted(aberto)));
	}

	private Long idDoRestaurante(String token) throws Exception {
		String resposta = mockMvc.perform(get("/api/v1/meu-restaurante").header("Authorization", bearer(token)))
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(resposta).get("id").asLong();
	}

	private String login(String email) throws Exception {
		String resposta = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"senha\":\"segredo123\"}".formatted(email)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(resposta).get("token").asText();
	}

	private String corpoItem(String nome, String preco, boolean disponivel) {
		return "{\"nome\":\"%s\",\"preco\":%s,\"disponivel\":%s}".formatted(nome, preco, disponivel);
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}

	private String emailUnico() {
		return "dono-" + UUID.randomUUID() + "@teste.com";
	}
}
