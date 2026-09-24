package com.pedeja.pagamento.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.pedeja.pagamento.entity.Pagamento;
import com.pedeja.pagamento.entity.StatusPagamento;

public record PagamentoResponse(
		Long id,
		Long pedidoId,
		StatusPagamento status,
		BigDecimal valor,
		String cobrancaId,
		String pixCopiaECola,
		Instant expiraEm
) {

	/**
	 * Uma cobrança pendente que passou do prazo aparece como EXPIRADO mesmo antes
	 * de alguém gravar isso no banco.
	 */
	public static PagamentoResponse from(Pagamento pagamento, Instant agora) {
		StatusPagamento status = pagamento.getStatus() == StatusPagamento.PENDENTE && !pagamento.aguardandoPagamento(agora)
				? StatusPagamento.EXPIRADO
				: pagamento.getStatus();

		return new PagamentoResponse(
				pagamento.getId(),
				pagamento.getPedidoId(),
				status,
				pagamento.getValor(),
				pagamento.getGatewayCobrancaId(),
				pagamento.getPixCopiaECola(),
				pagamento.getExpiraEm());
	}
}
