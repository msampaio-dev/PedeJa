package com.pedeja.pagamento.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class PagamentoEmAndamentoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PagamentoEmAndamentoException() {
		super("Há um pagamento em andamento para este pedido. Aguarde a confirmação ou o vencimento da cobrança");
	}
}
