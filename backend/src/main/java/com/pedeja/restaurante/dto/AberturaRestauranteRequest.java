package com.pedeja.restaurante.dto;

import jakarta.validation.constraints.NotNull;

public record AberturaRestauranteRequest(

		@NotNull(message = "Informe se o restaurante está aberto")
		Boolean aberto
) {
}
