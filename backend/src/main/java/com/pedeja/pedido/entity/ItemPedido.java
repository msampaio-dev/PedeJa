package com.pedeja.pedido.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "itens_pedido")
public class ItemPedido {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pedido_id", nullable = false, updatable = false)
	private Pedido pedido;

	@Column(name = "item_cardapio_id", nullable = false, updatable = false)
	private Long itemCardapioId;

	@Column(nullable = false, length = 120, updatable = false)
	private String nome;

	@Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2, updatable = false)
	private BigDecimal precoUnitario;

	@Column(nullable = false, updatable = false)
	private int quantidade;

	@Column(nullable = false, precision = 10, scale = 2, updatable = false)
	private BigDecimal subtotal;

	protected ItemPedido() {
	}

	ItemPedido(Pedido pedido, Long itemCardapioId, String nome, BigDecimal precoUnitario, int quantidade) {
		this.pedido = pedido;
		this.itemCardapioId = itemCardapioId;
		this.nome = nome;
		this.precoUnitario = precoUnitario;
		this.quantidade = quantidade;
		this.subtotal = precoUnitario.multiply(BigDecimal.valueOf(quantidade));
	}

	public Long getId() {
		return id;
	}

	public Long getItemCardapioId() {
		return itemCardapioId;
	}

	public String getNome() {
		return nome;
	}

	public BigDecimal getPrecoUnitario() {
		return precoUnitario;
	}

	public int getQuantidade() {
		return quantidade;
	}

	public BigDecimal getSubtotal() {
		return subtotal;
	}
}
