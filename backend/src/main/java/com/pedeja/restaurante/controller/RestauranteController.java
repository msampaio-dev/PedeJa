package com.pedeja.restaurante.controller;

import static com.pedeja.shared.web.ApiPaths.API_V1;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pedeja.restaurante.dto.CadastroRestauranteRequest;
import com.pedeja.restaurante.dto.CardapioPublicoResponse;
import com.pedeja.restaurante.dto.RestauranteResponse;
import com.pedeja.restaurante.entity.CategoriaRestaurante;
import com.pedeja.restaurante.service.RestauranteService;
import com.pedeja.shared.dto.PaginaResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Rotas públicas: como no app de verdade, dá para ver restaurantes e cardápios
 * sem login. A conta só é exigida na hora de pedir.
 */
@Validated
@RestController
@RequestMapping(API_V1 + "/restaurantes")
public class RestauranteController {

	private final RestauranteService restauranteService;

	public RestauranteController(RestauranteService restauranteService) {
		this.restauranteService = restauranteService;
	}

	@GetMapping
	@Operation(summary = "Listar restaurantes", security = {})
	public PaginaResponse<RestauranteResponse> listar(
			@RequestParam(required = false) CategoriaRestaurante categoria,
			@RequestParam(required = false) String busca,
			@RequestParam(defaultValue = "0") @Min(value = 0, message = "A página não pode ser negativa") int pagina,
			@RequestParam(defaultValue = "20") @Min(value = 1, message = "O tamanho mínimo é 1")
			@Max(value = 50, message = "O tamanho máximo é 50") int tamanho
	) {
		return restauranteService.listar(categoria, busca, pagina, tamanho);
	}

	@GetMapping("/{id}/cardapio")
	@Operation(summary = "Consultar o cardápio de um restaurante", security = {})
	public CardapioPublicoResponse cardapio(@PathVariable Long id) {
		return restauranteService.cardapioPublico(id);
	}

	@PostMapping("/cadastro")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Cadastrar restaurante com a conta do responsável", security = {})
	public RestauranteResponse cadastrar(@Valid @RequestBody CadastroRestauranteRequest request) {
		return restauranteService.cadastrar(request);
	}
}
