package com.pedeja.pedido.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * O cliente manda só o id e a quantidade de cada item. Preço, nome e taxa de
 * entrega saem do banco: valor vindo do navegador pode ter sido alterado.
 */
public record CriacaoPedidoRequest(

		@NotNull(message = "O restaurante é obrigatório")
		Long restauranteId,

		@NotEmpty(message = "O pedido precisa de ao menos um item")
		@Size(max = 30, message = "O pedido pode ter no máximo 30 itens diferentes")
		List<@Valid @NotNull ItemRequest> itens,

		@NotBlank(message = "O endereço de entrega é obrigatório")
		@Size(max = 300, message = "O endereço deve possuir no máximo 300 caracteres")
		String enderecoEntrega,

		@Size(max = 300, message = "A observação deve possuir no máximo 300 caracteres")
		String observacao
) {

	public record ItemRequest(

			@NotNull(message = "O item é obrigatório")
			Long itemId,

			@Min(value = 1, message = "A quantidade mínima é 1")
			@Max(value = 50, message = "A quantidade máxima é 50")
			int quantidade
	) {
	}
}
