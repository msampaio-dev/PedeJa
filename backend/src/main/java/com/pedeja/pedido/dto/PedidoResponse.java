package com.pedeja.pedido.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.pedeja.pedido.entity.HistoricoStatusPedido;
import com.pedeja.pedido.entity.ItemPedido;
import com.pedeja.pedido.entity.Pedido;
import com.pedeja.pedido.entity.StatusPedido;

public record PedidoResponse(
		Long id,
		StatusPedido status,
		RestauranteResumo restaurante,
		List<Item> itens,
		BigDecimal subtotal,
		BigDecimal taxaEntrega,
		BigDecimal total,
		String enderecoEntrega,
		String observacao,
		Instant criadoEm,
		List<Historico> historico
) {

	public record RestauranteResumo(Long id, String nome) {
	}

	public record Item(Long itemCardapioId, String nome, BigDecimal precoUnitario, int quantidade, BigDecimal subtotal) {

		static Item from(ItemPedido item) {
			return new Item(item.getItemCardapioId(), item.getNome(), item.getPrecoUnitario(),
					item.getQuantidade(), item.getSubtotal());
		}
	}

	public record Historico(StatusPedido status, Instant ocorridoEm) {

		static Historico from(HistoricoStatusPedido historico) {
			return new Historico(historico.getStatus(), historico.getOcorridoEm());
		}
	}

	public static PedidoResponse from(Pedido pedido) {
		return new PedidoResponse(
				pedido.getId(),
				pedido.getStatus(),
				new RestauranteResumo(pedido.getRestaurante().getId(), pedido.getRestaurante().getNome()),
				pedido.getItens().stream().map(Item::from).toList(),
				pedido.getSubtotal(),
				pedido.getTaxaEntrega(),
				pedido.getTotal(),
				pedido.getEnderecoEntrega(),
				pedido.getObservacao(),
				pedido.getCriadoEm(),
				pedido.getHistorico().stream().map(Historico::from).toList());
	}
}
