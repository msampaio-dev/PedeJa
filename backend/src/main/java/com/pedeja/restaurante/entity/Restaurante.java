package com.pedeja.restaurante.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "restaurantes")
public class Restaurante {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Guarda só o id do dono. O restaurante nunca precisa dos dados do usuário,
	// e um @OneToOne traria a entidade junto sem necessidade.
	@Column(name = "usuario_id", nullable = false, updatable = false)
	private Long usuarioId;

	@Column(nullable = false, length = 120)
	private String nome;

	@Column(length = 500)
	private String descricao;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CategoriaRestaurante categoria;

	@Column(name = "taxa_entrega", nullable = false, precision = 10, scale = 2)
	private BigDecimal taxaEntrega;

	@Column(name = "tempo_entrega_minutos", nullable = false)
	private int tempoEntregaMinutos;

	@Column(nullable = false)
	private boolean aberto;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm;

	protected Restaurante() {
	}

	/**
	 * Todo restaurante nasce fechado. Ele abre quando o dono decide, depois de
	 * montar o cardápio.
	 */
	public Restaurante(Long usuarioId, DadosRestaurante dados, Instant criadoEm) {
		this.usuarioId = usuarioId;
		this.criadoEm = criadoEm;
		this.aberto = false;
		atualizar(dados);
	}

	public void atualizar(DadosRestaurante dados) {
		this.nome = dados.nome().trim();
		this.descricao = dados.descricao() == null || dados.descricao().isBlank() ? null : dados.descricao().trim();
		this.categoria = dados.categoria();
		this.taxaEntrega = dados.taxaEntrega();
		this.tempoEntregaMinutos = dados.tempoEntregaMinutos();
	}

	public void definirAberto(boolean aberto) {
		this.aberto = aberto;
	}

	public Long getId() {
		return id;
	}

	public Long getUsuarioId() {
		return usuarioId;
	}

	public String getNome() {
		return nome;
	}

	public String getDescricao() {
		return descricao;
	}

	public CategoriaRestaurante getCategoria() {
		return categoria;
	}

	public BigDecimal getTaxaEntrega() {
		return taxaEntrega;
	}

	public int getTempoEntregaMinutos() {
		return tempoEntregaMinutos;
	}

	public boolean isAberto() {
		return aberto;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}
}
