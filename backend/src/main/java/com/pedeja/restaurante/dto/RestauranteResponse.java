package com.pedeja.restaurante.dto;

import java.math.BigDecimal;

import com.pedeja.restaurante.entity.CategoriaRestaurante;
import com.pedeja.restaurante.entity.Restaurante;

public record RestauranteResponse(
		Long id,
		String nome,
		String descricao,
		CategoriaRestaurante categoria,
		BigDecimal taxaEntrega,
		int tempoEntregaMinutos,
		boolean aberto
) {

	public static RestauranteResponse from(Restaurante restaurante) {
		return new RestauranteResponse(
				restaurante.getId(),
				restaurante.getNome(),
				restaurante.getDescricao(),
				restaurante.getCategoria(),
				restaurante.getTaxaEntrega(),
				restaurante.getTempoEntregaMinutos(),
				restaurante.isAberto());
	}
}
