package com.pedeja.temporeal;

import static com.pedeja.shared.web.ApiPaths.API_V1;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.swagger.v3.oas.annotations.Operation;

/**
 * Server-Sent Events: uma resposta HTTP que não termina, por onde o servidor
 * empurra avisos. Mais simples que WebSocket quando só o servidor fala.
 *
 * O EventSource do navegador não manda header Authorization, e o token na URL
 * vazaria em log de proxy. Por isso o frontend abre esta conexão com fetch,
 * que aceita o header, e lê o fluxo à mão.
 */
@RestController
@RequestMapping(API_V1 + "/eventos")
public class EventosController {

	private final CanalEventos canal;

	public EventosController(CanalEventos canal) {
		this.canal = canal;
	}

	@GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@Operation(summary = "Receber em tempo real as mudanças dos meus pedidos (SSE)")
	public SseEmitter conectar(@AuthenticationPrincipal Jwt jwt) {
		return canal.conectar(Long.valueOf(jwt.getSubject()));
	}
}
