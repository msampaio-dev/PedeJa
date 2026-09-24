package com.pedeja.temporeal;

import static com.pedeja.shared.Cenario.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedeja.shared.Cenario;
import com.pedeja.shared.ContainersIntegrationTest;

/**
 * Abre a conexão SSE por HTTP de verdade e confere que a mudança do pedido chega
 * por ela, depois de passar por outbox, RabbitMQ e fila anônima.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class TempoRealIntegrationTest extends ContainersIntegrationTest {

	private static final String FIM_DE_LINHA = "\r\n";

	@LocalServerPort
	private int porta;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CanalEventos canal;

	private final HttpClient http = HttpClient.newHttpClient();

	@Test
	void clienteConectadoRecebeAMudancaDoProprioPedido() throws Exception {
		Cenario cenario = new Cenario(mockMvc, objectMapper);
		String cliente = cenario.novoCliente();
		var restaurante = cenario.restauranteAberto("20.00");

		List<String> linhas = new CopyOnWriteArrayList<>();
		CompletableFuture<?> conexao = conectar(cliente, linhas);
		await().atMost(Duration.ofSeconds(5)).until(() -> linhas.contains("event:conectado"));

		long pedido = cenario.novoPedido(cliente, restaurante);

		await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(linhas)
				.contains("event:pedido")
				.anySatisfy(linha -> assertThat(linha)
						.contains("\"pedidoId\":" + pedido)
						.contains("AGUARDANDO_PAGAMENTO")));
		conexao.cancel(true);
	}

	@Test
	void recusaConexaoSemToken() throws Exception {
		HttpResponse<Void> resposta = http.send(
				HttpRequest.newBuilder(URI.create("http://localhost:%d/api/v1/eventos".formatted(porta))).GET().build(),
				HttpResponse.BodyHandlers.discarding());

		assertThat(resposta.statusCode()).isEqualTo(401);
	}

	@Test
	void conexaoFechadaPeloClienteSaiDoCanal() throws Exception {
		Cenario cenario = new Cenario(mockMvc, objectMapper);
		String cliente = cenario.novoCliente();
		int antes = canal.conexoesAbertas();

		// Socket cru: é o jeito de fechar a conexão TCP de verdade, como faz o
		// navegador ao fechar a aba. (Cancelar o HttpClient não derruba o socket.)
		try (Socket socket = new Socket("localhost", porta)) {
			String requisicao = String.join(FIM_DE_LINHA,
					"GET /api/v1/eventos HTTP/1.1",
					"Host: localhost",
					"Accept: text/event-stream",
					"Authorization: " + bearer(cliente),
					"",
					"");
			socket.getOutputStream().write(requisicao.getBytes(StandardCharsets.UTF_8));

			BufferedReader leitor = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
			String linha;
			while ((linha = leitor.readLine()) != null && !linha.equals("event:conectado")) {
				// Pula os cabeçalhos da resposta até o primeiro evento.
			}
			assertThat(canal.conexoesAbertas()).isEqualTo(antes + 1);
		}

		// O servidor só percebe a saída ao tentar escrever. O ping periódico faz isso.
		await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
			canal.manterConexoesVivas();
			assertThat(canal.conexoesAbertas()).isEqualTo(antes);
		});
	}

	private CompletableFuture<?> conectar(String token, List<String> linhas) {
		HttpRequest requisicao = HttpRequest.newBuilder(URI.create("http://localhost:%d/api/v1/eventos".formatted(porta)))
				.header("Authorization", bearer(token))
				.header("Accept", "text/event-stream")
				.GET()
				.build();
		return http.sendAsync(requisicao, HttpResponse.BodyHandlers.ofLines())
				.thenAccept(resposta -> {
					try (Stream<String> corpo = resposta.body()) {
						corpo.forEach(linhas::add);
					}
				});
	}
}
