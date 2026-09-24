package com.pedeja.cardapio.dto;

import java.math.BigDecimal;

import com.pedeja.cardapio.entity.ItemCardapio;

public record ItemCardapioResponse(
		Long id,
		String nome,
		String descricao,
		BigDecimal preco,
		boolean disponivel
) {

	public static ItemCardapioResponse from(ItemCardapio item) {
		return new ItemCardapioResponse(
				item.getId(),
				item.getNome(),
				item.getDescricao(),
				item.getPreco(),
				item.isDisponivel());
	}
}
