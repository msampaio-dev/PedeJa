package com.pedeja.cardapio.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.pedeja.restaurante.entity.Restaurante;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Item não tem exclusão: o dono marca como indisponível. Os pedidos da fase 3
 * vão apontar para o item, e apagá-lo quebraria o histórico.
 */
@Entity
@Table(name = "itens_cardapio")
public class ItemCardapio {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "restaurante_id", nullable = false, updatable = false)
	private Restaurante restaurante;

	@Column(nullable = false, length = 120)
	private String nome;

	@Column(length = 500)
	private String descricao;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal preco;

	@Column(nullable = false)
	private boolean disponivel;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm;

	protected ItemCardapio() {
	}

	public ItemCardapio(Restaurante restaurante, DadosItemCardapio dados, Instant criadoEm) {
		this.restaurante = restaurante;
		this.criadoEm = criadoEm;
		atualizar(dados);
	}

	public void atualizar(DadosItemCardapio dados) {
		this.nome = dados.nome().trim();
		this.descricao = dados.descricao() == null || dados.descricao().isBlank() ? null : dados.descricao().trim();
		this.preco = dados.preco();
		this.disponivel = dados.disponivel();
	}

	public Long getId() {
		return id;
	}

	public Restaurante getRestaurante() {
		return restaurante;
	}

	public String getNome() {
		return nome;
	}

	public String getDescricao() {
		return descricao;
	}

	public BigDecimal getPreco() {
		return preco;
	}

	public boolean isDisponivel() {
		return disponivel;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}
}
