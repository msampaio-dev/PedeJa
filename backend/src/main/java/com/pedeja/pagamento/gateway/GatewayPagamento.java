package com.pedeja.pagamento.gateway;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * O que a API precisa de um gateway de pagamento. Hoje só existe o GatewayFake;
 * plugar o Mercado Pago ou o Stripe é escrever outra implementação desta
 * interface, sem mexer em pedido nem em webhook.
 */
public interface GatewayPagamento {

	CobrancaCriada criarCobranca(NovaCobranca cobranca);

	record NovaCobranca(String referencia, BigDecimal valor, Instant expiraEm) {
	}

	record CobrancaCriada(String id, String pixCopiaECola) {
	}
}
