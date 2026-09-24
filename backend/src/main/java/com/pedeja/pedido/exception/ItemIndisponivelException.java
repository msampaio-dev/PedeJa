package com.pedeja.pedido.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class ItemIndisponivelException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ItemIndisponivelException(String mensagem) {
		super(mensagem);
	}
}
