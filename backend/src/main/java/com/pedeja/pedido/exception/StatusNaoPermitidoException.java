package com.pedeja.pedido.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.pedeja.pedido.entity.StatusPedido;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class StatusNaoPermitidoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public StatusNaoPermitidoException(StatusPedido status) {
		super("O restaurante não pode mudar o pedido para %s".formatted(status));
	}
}
