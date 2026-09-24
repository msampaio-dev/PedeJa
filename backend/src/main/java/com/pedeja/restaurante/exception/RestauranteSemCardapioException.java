package com.pedeja.restaurante.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class RestauranteSemCardapioException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public RestauranteSemCardapioException() {
		super("Cadastre ao menos um item disponível antes de abrir o restaurante");
	}
}
