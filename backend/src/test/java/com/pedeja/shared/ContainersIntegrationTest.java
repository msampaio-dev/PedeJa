package com.pedeja.shared;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

/**
 * Base dos testes de integração, com PostgreSQL e RabbitMQ reais.
 *
 * Os containers são singletons iniciados uma única vez por JVM e nunca parados
 * explicitamente: o Ryuk do Testcontainers os remove ao fim da execução. Não use
 * {@code @Testcontainers}/{@code @Container} aqui — a extensão do JUnit encerra o
 * container ao fim de cada classe de teste e o recria na seguinte, numa porta
 * nova, enquanto o Spring reaproveita o ApplicationContext em cache com as
 * conexões apontando para o container antigo.
 *
 * Cada configuração de contexto recebe um banco próprio dentro do PostgreSQL,
 * para as classes de teste partirem de um banco vazio. O RabbitMQ é
 * compartilhado: os testes de mensageria conferem pelo id do pedido, não pela
 * contagem de mensagens na fila.
 */
@ActiveProfiles("test")
@EnabledIf("com.pedeja.shared.ContainersIntegrationTest#dockerDisponivel")
public abstract class ContainersIntegrationTest {

	protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.15-alpine");
	protected static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:4-management-alpine");

	private static final AtomicInteger SEQUENCIA = new AtomicInteger();

	static {
		if (dockerDisponivel()) {
			POSTGRES.start();
			RABBITMQ.start();
		}
	}

	@DynamicPropertySource
	static void infraestrutura(DynamicPropertyRegistry registry) {
		String banco = criarBanco();
		registry.add("spring.datasource.url", () -> urlJdbc(banco));
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
		registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
		registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
		registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
		registry.add("app.mensageria.prefixo", () -> banco);
	}

	private static String criarBanco() {
		String banco = "pedeja_test_" + SEQUENCIA.incrementAndGet();
		try (Connection conexao = DriverManager.getConnection(
				POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
				Statement statement = conexao.createStatement()) {
			statement.execute("CREATE DATABASE " + banco);
		} catch (SQLException e) {
			throw new IllegalStateException("Não foi possível criar o banco de teste " + banco, e);
		}
		return banco;
	}

	private static String urlJdbc(String banco) {
		return "jdbc:postgresql://%s:%d/%s".formatted(
				POSTGRES.getHost(), POSTGRES.getFirstMappedPort(), banco);
	}

	public static boolean dockerDisponivel() {
		return DockerClientFactory.instance().isDockerAvailable();
	}
}
