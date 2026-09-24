package com.pedeja.restaurante.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class RestauranteNaoEncontradoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public RestauranteNaoEncontradoException() {
		super("Restaurante não encontrado");
	}
}
