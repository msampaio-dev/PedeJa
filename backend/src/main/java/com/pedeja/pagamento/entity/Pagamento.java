package com.pedeja.pagamento.entity;

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

/**
 * Uma tentativa de pagamento de um pedido, ligada a uma cobrança no gateway.
 * Quem decide se foi paga é o gateway, pelo webhook. A API nunca marca um
 * pagamento como aprovado por conta própria.
 */
@Entity
@Table(name = "pagamentos")
public class Pagamento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "pedido_id", nullable = false, updatable = false)
	private Long pedidoId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusPagamento status;

	@Column(nullable = false, precision = 10, scale = 2, updatable = false)
	private BigDecimal valor;

	@Column(name = "gateway_cobranca_id", nullable = false, length = 100, updatable = false)
	private String gatewayCobrancaId;

	@Column(name = "pix_copia_e_cola", nullable = false, length = 500, updatable = false)
	private String pixCopiaECola;

	@Column(name = "expira_em", nullable = false, updatable = false)
	private Instant expiraEm;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm;

	@Column(name = "atualizado_em", nullable = false)
	private Instant atualizadoEm;

	protected Pagamento() {
	}

	public Pagamento(
			Long pedidoId,
			BigDecimal valor,
			String gatewayCobrancaId,
			String pixCopiaECola,
			Instant expiraEm,
			Instant agora
	) {
		this.pedidoId = pedidoId;
		this.valor = valor;
		this.gatewayCobrancaId = gatewayCobrancaId;
		this.pixCopiaECola = pixCopiaECola;
		this.expiraEm = expiraEm;
		this.status = StatusPagamento.PENDENTE;
		this.criadoEm = agora;
		this.atualizadoEm = agora;
	}

	/** Pendente e ainda dentro do prazo: o cliente pode pagar esta cobrança. */
	public boolean aguardandoPagamento(Instant agora) {
		return status == StatusPagamento.PENDENTE && agora.isBefore(expiraEm);
	}

	public void aprovar(Instant agora) {
		mudar(StatusPagamento.APROVADO, agora);
	}

	public void recusar(Instant agora) {
		mudar(StatusPagamento.RECUSADO, agora);
	}

	public void expirar(Instant agora) {
		mudar(StatusPagamento.EXPIRADO, agora);
	}

	private void mudar(StatusPagamento novo, Instant agora) {
		this.status = novo;
		this.atualizadoEm = agora;
	}

	public Long getId() {
		return id;
	}

	public Long getPedidoId() {
		return pedidoId;
	}

	public StatusPagamento getStatus() {
		return status;
	}

	public BigDecimal getValor() {
		return valor;
	}

	public String getGatewayCobrancaId() {
		return gatewayCobrancaId;
	}

	public String getPixCopiaECola() {
		return pixCopiaECola;
	}

	public Instant getExpiraEm() {
		return expiraEm;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}
}
