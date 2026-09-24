package com.pedeja.pedido.entity;

import java.time.Instant;

/**
 * Evento de domínio: o pedido mudou de status (inclusive ao nascer, em
 * AGUARDANDO_PAGAMENTO). Leva os ids de quem precisa saber disso, para o
 * consumidor não ter que consultar o banco para descobrir.
 */
public record PedidoStatusAlterado(
		Long pedidoId,
		Long clienteId,
		Long restauranteId,
		Long donoRestauranteId,
		StatusPedido status,
		Instant ocorridoEm
) {
}
