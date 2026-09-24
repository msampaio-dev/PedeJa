package com.pedeja.restaurante.entity;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Os campos que o dono informa sobre o restaurante, iguais no cadastro e na
 * edição. Fica num record só para as duas requisições validarem do mesmo jeito.
 */
public record DadosRestaurante(

		@NotBlank(message = "O nome do restaurante é obrigatório")
		@Size(max = 120, message = "O nome do restaurante deve possuir no máximo 120 caracteres")
		String nome,

		@Size(max = 500, message = "A descrição deve possuir no máximo 500 caracteres")
		String descricao,

		@NotNull(message = "A categoria é obrigatória")
		CategoriaRestaurante categoria,

		@NotNull(message = "A taxa de entrega é obrigatória")
		@DecimalMin(value = "0.00", message = "A taxa de entrega não pode ser negativa")
		@Digits(integer = 8, fraction = 2, message = "A taxa de entrega deve ter no máximo 2 casas decimais")
		BigDecimal taxaEntrega,

		@Min(value = 5, message = "O tempo de entrega deve ser de pelo menos 5 minutos")
		@Max(value = 180, message = "O tempo de entrega deve ser de no máximo 180 minutos")
		int tempoEntregaMinutos
) {
}
