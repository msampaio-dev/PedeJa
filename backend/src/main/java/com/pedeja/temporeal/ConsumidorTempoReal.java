package com.pedeja.temporeal;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedeja.mensageria.MensagemPedido;
import com.pedeja.pedido.entity.StatusPedido;

/**
 * Repassa as mudanças de pedido para as telas abertas, por SSE.
 *
 * Usa a fila anônima declarada em TopologiaRabbit: exclusiva desta instância
 * e apagada quando ela cai. Com duas instâncias, cada uma tem a sua fila e recebe todos os eventos (é um
 * fanout), porque o cliente pode estar conectado em qualquer uma. A fila de
 * notificações é o oposto: uma fila só, dividida entre as instâncias, para cada
 * notificação ser gravada uma vez.
 *
 * Aviso perdido aqui não é grave: a tela também consulta o pedido de tempos em
 * tempos. Por isso não há DLQ nem retry próprio.
 */
@Component
@Lazy(false)
public class ConsumidorTempoReal {

	private static final Logger log = LoggerFactory.getLogger(ConsumidorTempoReal.class);

	private final CanalEventos canal;
	private final ObjectMapper objectMapper;

	public ConsumidorTempoReal(CanalEventos canal, ObjectMapper objectMapper) {
		this.canal = canal;
		this.objectMapper = objectMapper;
	}

	@RabbitListener(queues = "#{@filaTempoReal.name}")
	public void receber(Message mensagem) {
		MensagemPedido evento;
		try {
			evento = objectMapper.readValue(new String(mensagem.getBody(), StandardCharsets.UTF_8), MensagemPedido.class);
		} catch (JsonProcessingException e) {
			log.warn("Mensagem ilegível ignorada no tempo real");
			return;
		}

		var dados = Map.of("pedidoId", evento.pedidoId(), "status", evento.status());
		canal.publicar(evento.clienteId(), "pedido", dados);

		// Pedido não pago não existe para o restaurante.
		if (evento.status() != StatusPedido.AGUARDANDO_PAGAMENTO) {
			canal.publicar(evento.donoRestauranteId(), "pedido", dados);
		}
	}
}
