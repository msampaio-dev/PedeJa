package com.pedeja.pedido.entity;

import java.math.BigDecimal;

/** Um item já conferido contra o cardápio, com o preço vindo do banco. */
public record LinhaPedido(Long itemCardapioId, String nome, BigDecimal precoUnitario, int quantidade) {
}
