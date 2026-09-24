package com.pedeja.auth.dto;

import java.time.Instant;

import com.pedeja.auth.service.TokenGerado;

public record LoginResponse(
		String token,
		String tipo,
		Instant expiraEm,
		SessaoResponse usuario
) {

	public static LoginResponse from(TokenGerado token, SessaoResponse usuario) {
		return new LoginResponse(token.valor(), "Bearer", token.expiraEm(), usuario);
	}
}
