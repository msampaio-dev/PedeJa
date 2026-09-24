package com.pedeja.mensageria;

import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.LazyInitializationExcludeFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <pre>
 *                        pedido.status.#
 * pedeja.pedidos ──────────────────────────► pedeja.notificacoes ──(falhou 3x)──► pedeja.pedidos.dlx ──► pedeja.notificacoes.dlq
 *    (topic)
 * </pre>
 *
 * O Spring declara tudo isto no RabbitMQ na primeira conexão, e não recria o
 * que já existe. O prefixo ("pedeja") é configurável para os testes: cada
 * contexto usa filas próprias no mesmo broker e um não consome a mensagem do outro.
 */
@Configuration
public class TopologiaRabbit {

	public record Nomes(String exchangePedidos, String exchangeMortas, String filaNotificacoes, String filaNotificacoesMortas) {
	}

	/**
	 * Em produção a inicialização é preguiçosa (spring.main.lazy-initialization),
	 * e bean que ninguém pede não é criado. O RabbitAdmin e as filas, exchanges e
	 * bindings precisam existir desde o início: é o RabbitAdmin que os declara no
	 * broker quando a conexão abre.
	 */
	@Bean
	static LazyInitializationExcludeFilter mensageriaSempreCriada() {
		return LazyInitializationExcludeFilter.forBeanTypes(RabbitAdmin.class, Declarable.class);
	}

	@Bean
	Nomes nomesRabbit(@Value("${app.mensageria.prefixo:pedeja}") String prefixo) {
		return new Nomes(
				prefixo + ".pedidos",
				prefixo + ".pedidos.dlx",
				prefixo + ".notificacoes",
				prefixo + ".notificacoes.dlq");
	}

	@Bean
	TopicExchange exchangePedidos(Nomes nomes) {
		return new TopicExchange(nomes.exchangePedidos(), true, false);
	}

	@Bean
	DirectExchange exchangeMortas(Nomes nomes) {
		return new DirectExchange(nomes.exchangeMortas(), true, false);
	}

	/**
	 * Mensagem que falha todas as tentativas é rejeitada e o RabbitMQ a move para
	 * a DLQ, em vez de descartar ou de ficar voltando para a fila para sempre.
	 */
	@Bean
	Queue filaNotificacoes(Nomes nomes) {
		return QueueBuilder.durable(nomes.filaNotificacoes())
				.deadLetterExchange(nomes.exchangeMortas())
				.deadLetterRoutingKey(nomes.filaNotificacoes())
				.build();
	}

	@Bean
	Queue filaNotificacoesMortas(Nomes nomes) {
		return QueueBuilder.durable(nomes.filaNotificacoesMortas()).build();
	}

	@Bean
	Binding notificacoesRecebemStatus(Queue filaNotificacoes, TopicExchange exchangePedidos) {
		return BindingBuilder.bind(filaNotificacoes).to(exchangePedidos).with("pedido.status.#");
	}

	@Bean
	Binding mortasVaoParaDlq(Queue filaNotificacoesMortas, DirectExchange exchangeMortas, Nomes nomes) {
		return BindingBuilder.bind(filaNotificacoesMortas).to(exchangeMortas).with(nomes.filaNotificacoes());
	}

	/**
	 * Fila do tempo real (SSE): anônima, exclusiva desta instância e apagada
	 * quando ela cai. Cada instância recebe todos os eventos. Ver ConsumidorTempoReal.
	 */
	@Bean
	Queue filaTempoReal() {
		return new AnonymousQueue();
	}

	@Bean
	Binding tempoRealRecebeStatus(Queue filaTempoReal, TopicExchange exchangePedidos) {
		return BindingBuilder.bind(filaTempoReal).to(exchangePedidos).with("pedido.status.#");
	}
}
