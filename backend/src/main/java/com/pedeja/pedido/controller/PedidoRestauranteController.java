package com.pedeja.pedido.controller;

import static com.pedeja.shared.web.ApiPaths.API_V1;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pedeja.pedido.dto.MudancaStatusRequest;
import com.pedeja.pedido.dto.PedidoResponse;
import com.pedeja.pedido.service.PedidoService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping(API_V1 + "/meu-restaurante/pedidos")
public class PedidoRestauranteController {

	private final PedidoService pedidoService;

	public PedidoRestauranteController(PedidoService pedidoService) {
		this.pedidoService = pedidoService;
	}

	@GetMapping
	@Operation(summary = "Listar os pedidos em andamento do restaurante")
	public List<PedidoResponse> listar(@AuthenticationPrincipal Jwt jwt) {
		return pedidoService.listarDoRestaurante(usuarioId(jwt));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Consultar um pedido do restaurante")
	public PedidoResponse buscar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		return pedidoService.buscarDoRestaurante(usuarioId(jwt), id);
	}

	@PatchMapping("/{id}/status")
	@Operation(summary = "Aceitar, recusar ou avançar o status de um pedido")
	public PedidoResponse mudarStatus(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable Long id,
			@Valid @RequestBody MudancaStatusRequest request
	) {
		return pedidoService.mudarStatusPeloRestaurante(usuarioId(jwt), id, request.status());
	}

	private Long usuarioId(Jwt jwt) {
		return Long.valueOf(jwt.getSubject());
	}
}
