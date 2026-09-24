package com.pedeja.pedido.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.pedeja.pedido.entity.StatusPedido;

/** 409: o pedido existe, mas o estado atual dele não permite a mudança pedida. */
@ResponseStatus(HttpStatus.CONFLICT)
public class TransicaoStatusInvalidaException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public TransicaoStatusInvalidaException(StatusPedido atual, StatusPedido novo) {
		super("O pedido está %s e não pode passar para %s".formatted(atual, novo));
	}
}
