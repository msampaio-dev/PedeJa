package com.pedeja.mensageria;

import java.time.Instant;
import java.util.UUID;

import com.pedeja.pedido.entity.PedidoStatusAlterado;
import com.pedeja.pedido.entity.StatusPedido;

/**
 * Contrato da mensagem publicada no RabbitMQ. O eventoId viaja junto para os
 * consumidores descartarem entregas repetidas.
 */
public record MensagemPedido(
		UUID eventoId,
		Long pedidoId,
		Long clienteId,
		Long restauranteId,
		Long donoRestauranteId,
		StatusPedido status,
		Instant ocorridoEm
) {

	public static MensagemPedido de(PedidoStatusAlterado evento) {
		return new MensagemPedido(
				UUID.randomUUID(),
				evento.pedidoId(),
				evento.clienteId(),
				evento.restauranteId(),
				evento.donoRestauranteId(),
				evento.status(),
				evento.ocorridoEm());
	}

	/** Ex.: pedido.status.saiu_para_entrega. Consumidores assinam o que interessa com curinga. */
	public String routingKey() {
		return "pedido.status." + status.name().toLowerCase();
	}
}
