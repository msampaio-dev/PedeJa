package com.pedeja.pagamento.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class AssinaturaInvalidaException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public AssinaturaInvalidaException() {
		super("Assinatura do webhook inválida");
	}
}
