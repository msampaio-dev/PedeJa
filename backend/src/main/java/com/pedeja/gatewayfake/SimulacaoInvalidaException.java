package com.pedeja.gatewayfake;

public class SimulacaoInvalidaException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final int status;

	public SimulacaoInvalidaException(int status, String mensagem) {
		super(mensagem);
		this.status = status;
	}

	public int getStatus() {
		return status;
	}
}
