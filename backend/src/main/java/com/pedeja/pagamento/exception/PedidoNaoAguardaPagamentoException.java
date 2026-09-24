package com.pedeja.pagamento.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class PedidoNaoAguardaPagamentoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PedidoNaoAguardaPagamentoException() {
		super("Este pedido não está aguardando pagamento");
	}
}
