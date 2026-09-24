package com.pedeja.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.pedeja.usuario.entity.Usuario;

@Service
public class TokenService {

	private static final String EMISSOR = "pedeja";

	private final JwtEncoder jwtEncoder;
	private final Clock clock;
	private final long expirationMinutes;

	public TokenService(
			JwtEncoder jwtEncoder,
			Clock clock,
			@Value("${app.security.jwt.expiration-minutes}") long expirationMinutes
	) {
		this.jwtEncoder = jwtEncoder;
		this.clock = clock;
		this.expirationMinutes = expirationMinutes;
	}

	public TokenGerado gerar(Usuario usuario) {
		Instant emitidoEm = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS);
		Instant expiraEm = emitidoEm.plus(expirationMinutes, ChronoUnit.MINUTES);

		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(EMISSOR)
				.issuedAt(emitidoEm)
				.expiresAt(expiraEm)
				.subject(usuario.getId().toString())
				.id(UUID.randomUUID().toString())
				.claim("perfil", usuario.getPerfil().name())
				.build();

		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String valor = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

		return new TokenGerado(valor, expiraEm);
	}
}
