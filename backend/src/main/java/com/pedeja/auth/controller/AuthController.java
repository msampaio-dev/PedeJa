package com.pedeja.auth.controller;

import static com.pedeja.shared.web.ApiPaths.API_V1;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pedeja.auth.dto.CadastroClienteRequest;
import com.pedeja.auth.dto.LoginRequest;
import com.pedeja.auth.dto.LoginResponse;
import com.pedeja.auth.dto.SessaoResponse;
import com.pedeja.auth.service.AuthService;
import com.pedeja.auth.service.TokenService;
import com.pedeja.usuario.entity.Usuario;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping(API_V1 + "/auth")
public class AuthController {

	private final AuthService authService;
	private final TokenService tokenService;

	public AuthController(AuthService authService, TokenService tokenService) {
		this.authService = authService;
		this.tokenService = tokenService;
	}

	@PostMapping("/cadastro")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Criar conta de cliente", security = {})
	public SessaoResponse cadastrar(@Valid @RequestBody CadastroClienteRequest request) {
		return authService.cadastrarCliente(request);
	}

	@PostMapping("/login")
	@Operation(summary = "Autenticar usuário", security = {})
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		Usuario usuario = authService.autenticar(request.email(), request.senha());
		return LoginResponse.from(tokenService.gerar(usuario), SessaoResponse.from(usuario));
	}

	@GetMapping("/me")
	@Operation(summary = "Consultar o usuário autenticado")
	public SessaoResponse me(@AuthenticationPrincipal Jwt jwt) {
		return authService.buscarSessao(Long.valueOf(jwt.getSubject()));
	}
}
