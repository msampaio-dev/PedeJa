package com.pedeja.pedido.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "historico_status_pedido")
public class HistoricoStatusPedido {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pedido_id", nullable = false, updatable = false)
	private Pedido pedido;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30, updatable = false)
	private StatusPedido status;

	@Column(name = "ocorrido_em", nullable = false, updatable = false)
	private Instant ocorridoEm;

	protected HistoricoStatusPedido() {
	}

	HistoricoStatusPedido(Pedido pedido, StatusPedido status, Instant ocorridoEm) {
		this.pedido = pedido;
		this.status = status;
		this.ocorridoEm = ocorridoEm;
	}

	public StatusPedido getStatus() {
		return status;
	}

	public Instant getOcorridoEm() {
		return ocorridoEm;
	}
}
