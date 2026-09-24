package com.pedeja.pedido.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PedidoNaoEncontradoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PedidoNaoEncontradoException() {
		super("Pedido não encontrado");
	}
}
