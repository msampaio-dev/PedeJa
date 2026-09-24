package com.pedeja.pedido.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class RestauranteFechadoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public RestauranteFechadoException() {
		super("O restaurante está fechado e não aceita pedidos agora");
	}
}
