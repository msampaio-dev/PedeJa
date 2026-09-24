package com.pedeja.pedido.entity;

import java.util.Set;

/**
 * Máquina de estados do pedido.
 *
 * <pre>
 * AGUARDANDO_PAGAMENTO ──► PAGO ──► ACEITO ──► EM_PREPARO ──► SAIU_PARA_ENTREGA ──► ENTREGUE
 *          │                 │
 *          ▼                 ▼
 *      CANCELADO          RECUSADO
 * </pre>
 *
 * Cancelar só vale antes do pagamento. Depois disso quem decide é o restaurante:
 * aceita ou recusa.
 */
public enum StatusPedido {
	AGUARDANDO_PAGAMENTO,
	PAGO,
	ACEITO,
	EM_PREPARO,
	SAIU_PARA_ENTREGA,
	ENTREGUE,
	CANCELADO,
	RECUSADO;

	public Set<StatusPedido> proximosPermitidos() {
		return switch (this) {
			case AGUARDANDO_PAGAMENTO -> Set.of(PAGO, CANCELADO);
			case PAGO -> Set.of(ACEITO, RECUSADO);
			case ACEITO -> Set.of(EM_PREPARO);
			case EM_PREPARO -> Set.of(SAIU_PARA_ENTREGA);
			case SAIU_PARA_ENTREGA -> Set.of(ENTREGUE);
			case ENTREGUE, CANCELADO, RECUSADO -> Set.of();
		};
	}

	public boolean podeIrPara(StatusPedido novo) {
		return proximosPermitidos().contains(novo);
	}

	/** Os status que o restaurante pode aplicar. PAGO vem do pagamento e CANCELADO, do cliente. */
	public static final Set<StatusPedido> DEFINIDOS_PELO_RESTAURANTE =
			Set.of(ACEITO, RECUSADO, EM_PREPARO, SAIU_PARA_ENTREGA, ENTREGUE);

	/** Pedidos que o restaurante ainda precisa tocar. Não pago não aparece para ele. */
	public static final Set<StatusPedido> EM_ANDAMENTO_NO_RESTAURANTE =
			Set.of(PAGO, ACEITO, EM_PREPARO, SAIU_PARA_ENTREGA);
}
