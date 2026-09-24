package com.pedeja.notificacao;

import java.util.ArrayList;
import java.util.List;

import com.pedeja.mensageria.MensagemPedido;
import com.pedeja.pedido.entity.StatusPedido;

/** Decide quem é avisado de cada mudança e com qual texto. */
final class TextoNotificacao {

	record Destino(Long usuarioId, String mensagem) {
	}

	private TextoNotificacao() {
	}

	static List<Destino> para(MensagemPedido evento) {
		long pedido = evento.pedidoId();
		List<Destino> destinos = new ArrayList<>();

		String paraCliente = switch (evento.status()) {
			case AGUARDANDO_PAGAMENTO -> "Recebemos o pedido #%d. Falta só o pagamento.".formatted(pedido);
			case PAGO -> "Pagamento do pedido #%d confirmado. Agora é com o restaurante.".formatted(pedido);
			case ACEITO -> "O restaurante aceitou o pedido #%d.".formatted(pedido);
			case EM_PREPARO -> "O pedido #%d está sendo preparado.".formatted(pedido);
			case SAIU_PARA_ENTREGA -> "O pedido #%d saiu para entrega.".formatted(pedido);
			case ENTREGUE -> "Pedido #%d entregue. Bom apetite!".formatted(pedido);
			case CANCELADO -> "O pedido #%d foi cancelado.".formatted(pedido);
			case RECUSADO -> "O restaurante recusou o pedido #%d.".formatted(pedido);
		};
		destinos.add(new Destino(evento.clienteId(), paraCliente));

		// O restaurante só fica sabendo do pedido depois de pago.
		if (evento.status() == StatusPedido.PAGO) {
			destinos.add(new Destino(evento.donoRestauranteId(), "Novo pedido #%d pago. Aceite ou recuse.".formatted(pedido)));
		}

		return destinos;
	}
}
