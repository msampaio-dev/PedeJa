package com.pedeja.restaurante.controller;

import static com.pedeja.shared.web.ApiPaths.API_V1;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pedeja.cardapio.dto.ItemCardapioResponse;
import com.pedeja.cardapio.entity.DadosItemCardapio;
import com.pedeja.cardapio.service.CardapioService;
import com.pedeja.restaurante.dto.AberturaRestauranteRequest;
import com.pedeja.restaurante.dto.RestauranteResponse;
import com.pedeja.restaurante.entity.DadosRestaurante;
import com.pedeja.restaurante.service.RestauranteService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

/**
 * Área do dono. Nenhuma rota recebe o id do restaurante: ele sai do token, e o
 * SecurityConfig só deixa entrar quem tem o perfil RESTAURANTE.
 */
@RestController
@RequestMapping(API_V1 + "/meu-restaurante")
public class MeuRestauranteController {

	private final RestauranteService restauranteService;
	private final CardapioService cardapioService;

	public MeuRestauranteController(RestauranteService restauranteService, CardapioService cardapioService) {
		this.restauranteService = restauranteService;
		this.cardapioService = cardapioService;
	}

	@GetMapping
	@Operation(summary = "Consultar o próprio restaurante")
	public RestauranteResponse buscar(@AuthenticationPrincipal Jwt jwt) {
		return restauranteService.buscarDoDono(usuarioId(jwt));
	}

	@PutMapping
	@Operation(summary = "Atualizar os dados do próprio restaurante")
	public RestauranteResponse atualizar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DadosRestaurante dados) {
		return restauranteService.atualizar(usuarioId(jwt), dados);
	}

	@PatchMapping("/abertura")
	@Operation(summary = "Abrir ou fechar o restaurante")
	public RestauranteResponse definirAbertura(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody AberturaRestauranteRequest request
	) {
		return restauranteService.definirAberto(usuarioId(jwt), request.aberto());
	}

	@GetMapping("/itens")
	@Operation(summary = "Listar todos os itens do próprio cardápio")
	public List<ItemCardapioResponse> listarItens(@AuthenticationPrincipal Jwt jwt) {
		return cardapioService.listarDoDono(usuarioId(jwt));
	}

	@PostMapping("/itens")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Adicionar item ao cardápio")
	public ItemCardapioResponse criarItem(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DadosItemCardapio dados) {
		return cardapioService.criar(usuarioId(jwt), dados);
	}

	@PutMapping("/itens/{id}")
	@Operation(summary = "Atualizar item do cardápio")
	public ItemCardapioResponse atualizarItem(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable Long id,
			@Valid @RequestBody DadosItemCardapio dados
	) {
		return cardapioService.atualizar(usuarioId(jwt), id, dados);
	}

	private Long usuarioId(Jwt jwt) {
		return Long.valueOf(jwt.getSubject());
	}
}
