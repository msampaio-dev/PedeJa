package com.pedeja.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedeja.shared.PostgresIntegrationTest;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest extends PostgresIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void deveCadastrarClienteEEntrarComAMesmaSenha() throws Exception {
		String email = emailUnico();
		cadastrar("Maria", email, "segredo123");

		String token = login(email.toUpperCase(), "segredo123");

		mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.perfil").value("CLIENTE"));
	}

	@Test
	void deveRecusarEmailJaCadastradoMesmoComOutraCaixa() throws Exception {
		String email = emailUnico();
		cadastrar("Maria", email, "segredo123");

		mockMvc.perform(post("/api/v1/auth/cadastro")
				.contentType(MediaType.APPLICATION_JSON)
				.content(corpoCadastro("Outra Maria", email.toUpperCase(), "segredo123")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensagem").value("E-mail já cadastrado"));
	}

	@Test
	void deveResponderComMensagemGenericaQuandoASenhaEstiverErrada() throws Exception {
		String email = emailUnico();
		cadastrar("Maria", email, "segredo123");

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"senha\":\"errada123\"}".formatted(email)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos"));
	}

	@Test
	void deveListarOsCamposInvalidosNoCadastro() throws Exception {
		mockMvc.perform(post("/api/v1/auth/cadastro")
				.contentType(MediaType.APPLICATION_JSON)
				.content(corpoCadastro("", "nao-e-email", "123")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos.length()").value(3));
	}

	@Test
	void deveExigirTokenNasRotasProtegidas() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.caminho").value("/api/v1/auth/me"));
	}

	private void cadastrar(String nome, String email, String senha) throws Exception {
		mockMvc.perform(post("/api/v1/auth/cadastro")
				.contentType(MediaType.APPLICATION_JSON)
				.content(corpoCadastro(nome, email, senha)))
				.andExpect(status().isCreated());
	}

	private String login(String email, String senha) throws Exception {
		String resposta = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		JsonNode json = objectMapper.readTree(resposta);
		return json.get("token").asText();
	}

	private String corpoCadastro(String nome, String email, String senha) {
		return "{\"nome\":\"%s\",\"email\":\"%s\",\"senha\":\"%s\"}".formatted(nome, email, senha);
	}

	private String emailUnico() {
		return "cliente-" + UUID.randomUUID() + "@teste.com";
	}
}
