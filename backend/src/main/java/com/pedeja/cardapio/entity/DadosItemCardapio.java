package com.pedeja.cardapio.entity;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DadosItemCardapio(

		@NotBlank(message = "O nome do item é obrigatório")
		@Size(max = 120, message = "O nome do item deve possuir no máximo 120 caracteres")
		String nome,

		@Size(max = 500, message = "A descrição deve possuir no máximo 500 caracteres")
		String descricao,

		// BigDecimal e não double: 0.1 + 0.2 em double dá 0.30000000000000004,
		// e o total do pedido não pode ter centavo fantasma.
		@NotNull(message = "O preço é obrigatório")
		@DecimalMin(value = "0.01", message = "O preço deve ser maior que zero")
		@Digits(integer = 8, fraction = 2, message = "O preço deve ter no máximo 2 casas decimais")
		BigDecimal preco,

		boolean disponivel
) {
}
