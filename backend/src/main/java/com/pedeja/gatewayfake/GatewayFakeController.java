package com.pedeja.gatewayfake;

import static com.pedeja.shared.web.ApiPaths.API_V1;

import java.time.Instant;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pedeja.pagamento.webhook.EventoPagamento.Resultado;
import com.pedeja.shared.exception.ErroApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * O "aplicativo do banco" do sandbox. Numa integração real, este endpoint não
 * existe: quem paga é o cliente, no app do banco dele.
 */
@RestController
@RequestMapping(API_V1 + "/gateway-fake")
@Tag(name = "Gateway falso (sandbox)")
@ConditionalOnProperty(name = "app.gateway-fake.enabled", havingValue = "true")
public class GatewayFakeController {

	private final GatewayFake gateway;

	public GatewayFakeController(GatewayFake gateway) {
		this.gateway = gateway;
	}

	public record SimulacaoRequest(@NotNull(message = "O resultado é obrigatório") Resultado resultado) {
	}

	@PostMapping("/cobrancas/{cobrancaId}/simulacao")
	@Operation(summary = "Simular o pagamento ou a recusa de uma cobrança", security = {})
	public ResponseEntity<Void> simular(@PathVariable String cobrancaId, @Valid @RequestBody SimulacaoRequest request) {
		gateway.simular(cobrancaId, request.resultado());
		return ResponseEntity.accepted().build();
	}

	@ExceptionHandler(SimulacaoInvalidaException.class)
	ResponseEntity<ErroApiResponse> tratar(SimulacaoInvalidaException exception, HttpServletRequest request) {
		HttpStatus status = HttpStatus.valueOf(exception.getStatus());
		return ResponseEntity.status(status).body(new ErroApiResponse(
				Instant.now(), status.value(), status.getReasonPhrase(), exception.getMessage(),
				request.getRequestURI(), List.of()));
	}
}
