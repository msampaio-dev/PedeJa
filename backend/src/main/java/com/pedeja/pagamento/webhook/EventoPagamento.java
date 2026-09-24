package com.pedeja.pagamento.webhook;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Corpo da notificação que o gateway manda para o webhook. */
public record EventoPagamento(

		@NotBlank
		String eventoId,

		@NotBlank
		String cobrancaId,

		@NotNull
		Resultado resultado,

		@NotNull
		Instant ocorridoEm
) {

	public enum Resultado {
		APROVADO,
		RECUSADO
	}
}
