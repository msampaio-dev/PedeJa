package com.pedeja.pagamento.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PagamentoNaoEncontradoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PagamentoNaoEncontradoException() {
		super("Nenhum pagamento foi iniciado para este pedido");
	}
}
