package com.pedeja.pedido.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.pedeja.pedido.entity.Pedido;
import com.pedeja.pedido.entity.StatusPedido;

public record PedidoResumoResponse(
		Long id,
		StatusPedido status,
		String restauranteNome,
		BigDecimal total,
		Instant criadoEm
) {

	public static PedidoResumoResponse from(Pedido pedido) {
		return new PedidoResumoResponse(
				pedido.getId(),
				pedido.getStatus(),
				pedido.getRestaurante().getNome(),
				pedido.getTotal(),
				pedido.getCriadoEm());
	}
}
