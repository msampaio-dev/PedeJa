package com.pedeja.pagamento.controller;

import static com.pedeja.shared.web.ApiPaths.API_V1;

import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedeja.pagamento.exception.AssinaturaInvalidaException;
import com.pedeja.pagamento.service.PagamentoService;
import com.pedeja.pagamento.webhook.AssinaturaWebhook;
import com.pedeja.pagamento.webhook.EventoPagamento;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * Chamado pelo gateway, não por usuário: não tem JWT, e a autenticação é a
 * assinatura HMAC do corpo.
 */
@RestController
@RequestMapping(API_V1 + "/webhooks/pagamentos")
public class WebhookPagamentoController {

	private final AssinaturaWebhook assinatura;
	private final PagamentoService pagamentoService;
	private final ObjectMapper objectMapper;
	private final Validator validator;

	public WebhookPagamentoController(
			AssinaturaWebhook assinatura,
			PagamentoService pagamentoService,
			ObjectMapper objectMapper,
			Validator validator
	) {
		this.assinatura = assinatura;
		this.pagamentoService = pagamentoService;
		this.objectMapper = objectMapper;
		this.validator = validator;
	}

	/**
	 * O corpo chega como texto puro porque a assinatura é calculada sobre os bytes
	 * exatos que o gateway enviou. Converter para objeto antes e serializar de novo
	 * poderia mudar espaços ou a ordem dos campos, e a assinatura não bateria.
	 */
	@PostMapping
	@Operation(summary = "Receber notificação de pagamento do gateway", security = {})
	public ResponseEntity<Void> receber(
			@RequestHeader(name = AssinaturaWebhook.HEADER, required = false) String cabecalhoAssinatura,
			@RequestBody String corpo
	) {
		if (!assinatura.valida(cabecalhoAssinatura, corpo)) {
			throw new AssinaturaInvalidaException();
		}

		// Corpo malformado responde 400, e não 500: erro 5xx faz o gateway tentar
		// de novo, e reenviar o mesmo corpo quebrado não adianta.
		EventoPagamento evento;
		try {
			evento = objectMapper.readValue(corpo, EventoPagamento.class);
		} catch (JsonProcessingException e) {
			return ResponseEntity.badRequest().build();
		}
		Set<ConstraintViolation<EventoPagamento>> violacoes = validator.validate(evento);
		if (!violacoes.isEmpty()) {
			return ResponseEntity.badRequest().build();
		}

		pagamentoService.processar(evento);
		return ResponseEntity.ok().build();
	}
}
