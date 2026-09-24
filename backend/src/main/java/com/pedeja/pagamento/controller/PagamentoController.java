package com.pedeja.pagamento.controller;

import static com.pedeja.shared.web.ApiPaths.API_V1;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pedeja.pagamento.dto.PagamentoResponse;
import com.pedeja.pagamento.service.PagamentoService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping(API_V1 + "/pedidos/{pedidoId}/pagamento")
public class PagamentoController {

	private final PagamentoService pagamentoService;

	public PagamentoController(PagamentoService pagamentoService) {
		this.pagamentoService = pagamentoService;
	}

	@PostMapping
	@Operation(summary = "Gerar a cobrança Pix do pedido (ou devolver a que ainda vale)")
	public PagamentoResponse iniciar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long pedidoId) {
		return pagamentoService.iniciar(Long.valueOf(jwt.getSubject()), pedidoId);
	}

	@GetMapping
	@Operation(summary = "Consultar a última tentativa de pagamento do pedido")
	public PagamentoResponse buscar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long pedidoId) {
		return pagamentoService.buscarUltimo(Long.valueOf(jwt.getSubject()), pedidoId);
	}
}
