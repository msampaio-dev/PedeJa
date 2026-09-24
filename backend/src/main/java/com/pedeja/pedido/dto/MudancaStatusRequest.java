package com.pedeja.pedido.dto;

import com.pedeja.pedido.entity.StatusPedido;

import jakarta.validation.constraints.NotNull;

public record MudancaStatusRequest(

		@NotNull(message = "O status é obrigatório")
		StatusPedido status
) {
}
