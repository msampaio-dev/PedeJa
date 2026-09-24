package com.pedeja.outbox;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "outbox_eventos")
public class OutboxEvento {

	private static final int TAMANHO_MAXIMO_ERRO = 500;

	@Id
	private UUID id;

	@Column(nullable = false, length = 60, updatable = false)
	private String tipo;

	@Column(name = "routing_key", nullable = false, length = 100, updatable = false)
	private String routingKey;

	@Column(name = "pedido_id", nullable = false, updatable = false)
	private Long pedidoId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, updatable = false, columnDefinition = "jsonb")
	private String payload;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm;

	@Column(name = "publicado_em")
	private Instant publicadoEm;

	@Column(nullable = false)
	private int tentativas;

	@Column(name = "ultimo_erro", length = TAMANHO_MAXIMO_ERRO)
	private String ultimoErro;

	protected OutboxEvento() {
	}

	public OutboxEvento(UUID id, String tipo, String routingKey, Long pedidoId, String payload, Instant criadoEm) {
		this.id = id;
		this.tipo = tipo;
		this.routingKey = routingKey;
		this.pedidoId = pedidoId;
		this.payload = payload;
		this.criadoEm = criadoEm;
	}

	public void marcarPublicado(Instant agora) {
		this.publicadoEm = agora;
		this.tentativas++;
		this.ultimoErro = null;
	}

	public void registrarFalha(String erro) {
		this.tentativas++;
		this.ultimoErro = erro == null ? null : erro.substring(0, Math.min(erro.length(), TAMANHO_MAXIMO_ERRO));
	}

	public UUID getId() {
		return id;
	}

	public String getRoutingKey() {
		return routingKey;
	}

	public String getPayload() {
		return payload;
	}

	public Instant getPublicadoEm() {
		return publicadoEm;
	}
}
