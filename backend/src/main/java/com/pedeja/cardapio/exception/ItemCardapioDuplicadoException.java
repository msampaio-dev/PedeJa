package com.pedeja.cardapio.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ItemCardapioDuplicadoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ItemCardapioDuplicadoException() {
		super("Já existe um item com esse nome no cardápio");
	}
}
