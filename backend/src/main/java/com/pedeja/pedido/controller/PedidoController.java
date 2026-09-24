package com.pedeja.pedido.controller;

import static com.pedeja.shared.web.ApiPaths.API_V1;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pedeja.pedido.dto.CriacaoPedidoRequest;
import com.pedeja.pedido.dto.PedidoResponse;
import com.pedeja.pedido.dto.PedidoResumoResponse;
import com.pedeja.pedido.service.PedidoService;
import com.pedeja.shared.dto.PaginaResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Validated
@RestController
@RequestMapping(API_V1 + "/pedidos")
public class PedidoController {

	private final PedidoService pedidoService;

	public PedidoController(PedidoService pedidoService) {
		this.pedidoService = pedidoService;
	}

	/**
	 * 201 quando cria. 200 quando a chave de idempotência já tinha gerado um
	 * pedido: o cliente recebe o mesmo pedido de antes.
	 */
	@PostMapping
	@Operation(summary = "Fazer pedido")
	public ResponseEntity<PedidoResponse> criar(
			@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador único da tentativa, gerado pelo cliente")
			@RequestHeader(name = "Idempotency-Key", required = false)
			@Size(min = 8, max = 100, message = "A chave de idempotência deve ter entre 8 e 100 caracteres")
			String chaveIdempotencia,
			@Valid @RequestBody CriacaoPedidoRequest request
	) {
		var resultado = pedidoService.criar(usuarioId(jwt), request, chaveIdempotencia);

		if (!resultado.novo()) {
			return ResponseEntity.ok(resultado.pedido());
		}
		return ResponseEntity
				.created(URI.create(API_V1 + "/pedidos/" + resultado.pedido().id()))
				.body(resultado.pedido());
	}

	@GetMapping
	@Operation(summary = "Listar meus pedidos")
	public PaginaResponse<PedidoResumoResponse> listar(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") @Min(value = 0, message = "A página não pode ser negativa") int pagina,
			@RequestParam(defaultValue = "20") @Min(value = 1, message = "O tamanho mínimo é 1")
			@Max(value = 50, message = "O tamanho máximo é 50") int tamanho
	) {
		return pedidoService.listarDoCliente(usuarioId(jwt), pagina, tamanho);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Consultar um pedido meu")
	public PedidoResponse buscar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		return pedidoService.buscarDoCliente(usuarioId(jwt), id);
	}

	@PostMapping("/{id}/cancelamento")
	@Operation(summary = "Cancelar pedido ainda não pago")
	public PedidoResponse cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		return pedidoService.cancelarPeloCliente(usuarioId(jwt), id);
	}

	private Long usuarioId(Jwt jwt) {
		return Long.valueOf(jwt.getSubject());
	}
}
