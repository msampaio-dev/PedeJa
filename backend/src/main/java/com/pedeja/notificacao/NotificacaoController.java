package com.pedeja.notificacao;

import static com.pedeja.shared.web.ApiPaths.API_V1;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pedeja.notificacao.NotificacaoRepository.Notificacao;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping(API_V1 + "/notificacoes")
public class NotificacaoController {

	private static final int LIMITE = 30;

	private final NotificacaoRepository notificacaoRepository;

	public NotificacaoController(NotificacaoRepository notificacaoRepository) {
		this.notificacaoRepository = notificacaoRepository;
	}

	@GetMapping
	@Operation(summary = "Listar minhas notificações mais recentes")
	public List<Notificacao> listar(@AuthenticationPrincipal Jwt jwt) {
		return notificacaoRepository.ultimas(Long.valueOf(jwt.getSubject()), LIMITE);
	}

	@PostMapping("/lidas")
	@Operation(summary = "Marcar todas as minhas notificações como lidas")
	public ResponseEntity<Void> marcarLidas(@AuthenticationPrincipal Jwt jwt) {
		notificacaoRepository.marcarTodasComoLidas(Long.valueOf(jwt.getSubject()));
		return ResponseEntity.noContent().build();
	}
}
