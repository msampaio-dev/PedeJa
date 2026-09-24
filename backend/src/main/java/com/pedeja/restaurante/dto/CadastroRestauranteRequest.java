package com.pedeja.restaurante.dto;

import com.pedeja.restaurante.entity.DadosRestaurante;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cria a conta do responsável e o restaurante numa requisição só. Um restaurante
 * sem dono, ou uma conta RESTAURANTE sem restaurante, não fazem sentido.
 */
public record CadastroRestauranteRequest(

		@NotBlank(message = "O nome do responsável é obrigatório")
		@Size(max = 120, message = "O nome do responsável deve possuir no máximo 120 caracteres")
		String nomeResponsavel,

		@NotBlank(message = "O e-mail é obrigatório")
		@Email(message = "O e-mail deve ser válido")
		@Size(max = 254, message = "O e-mail deve possuir no máximo 254 caracteres")
		String email,

		@NotBlank(message = "A senha é obrigatória")
		@Size(min = 6, max = 72, message = "A senha deve possuir entre 6 e 72 caracteres")
		String senha,

		@NotNull(message = "Os dados do restaurante são obrigatórios")
		@Valid
		DadosRestaurante restaurante
) {
}
