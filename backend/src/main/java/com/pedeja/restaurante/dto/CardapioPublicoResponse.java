package com.pedeja.restaurante.dto;

import java.util.List;

import com.pedeja.cardapio.dto.ItemCardapioResponse;

public record CardapioPublicoResponse(
		RestauranteResponse restaurante,
		List<ItemCardapioResponse> itens
) {
}
