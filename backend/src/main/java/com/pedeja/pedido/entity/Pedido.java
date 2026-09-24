package com.pedeja.pedido.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;

import com.pedeja.pedido.exception.TransicaoStatusInvalidaException;
import com.pedeja.restaurante.entity.Restaurante;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "pedidos")
public class Pedido {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "cliente_id", nullable = false, updatable = false)
	private Long clienteId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "restaurante_id", nullable = false, updatable = false)
	private Restaurante restaurante;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private StatusPedido status;

	@Column(nullable = false, precision = 10, scale = 2, updatable = false)
	private BigDecimal subtotal;

	@Column(name = "taxa_entrega", nullable = false, precision = 10, scale = 2, updatable = false)
	private BigDecimal taxaEntrega;

	@Column(nullable = false, precision = 10, scale = 2, updatable = false)
	private BigDecimal total;

	@Column(name = "endereco_entrega", nullable = false, length = 300, updatable = false)
	private String enderecoEntrega;

	@Column(length = 300, updatable = false)
	private String observacao;

	@Column(name = "chave_idempotencia", length = 100, updatable = false)
	private String chaveIdempotencia;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm;

	@Column(name = "atualizado_em", nullable = false)
	private Instant atualizadoEm;

	@OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL)
	@OrderBy("id")
	private List<ItemPedido> itens = new ArrayList<>();

	@OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL)
	@OrderBy("id")
	private List<HistoricoStatusPedido> historico = new ArrayList<>();

	// Mudanças ainda não anunciadas. Não vão para o banco: viram eventos quando o
	// repositório salva o pedido, e a lista é limpa em seguida.
	@Transient
	private final List<HistoricoStatusPedido> mudancasNaoPublicadas = new ArrayList<>();

	protected Pedido() {
	}

	/**
	 * A taxa de entrega também é copiada do restaurante agora, pelo mesmo motivo
	 * do preço dos itens.
	 */
	public Pedido(
			Long clienteId,
			Restaurante restaurante,
			List<LinhaPedido> linhas,
			String enderecoEntrega,
			String observacao,
			String chaveIdempotencia,
			Instant agora
	) {
		this.clienteId = clienteId;
		this.restaurante = restaurante;
		this.enderecoEntrega = enderecoEntrega.trim();
		this.observacao = observacao == null || observacao.isBlank() ? null : observacao.trim();
		this.chaveIdempotencia = chaveIdempotencia;
		this.criadoEm = agora;

		for (LinhaPedido linha : linhas) {
			itens.add(new ItemPedido(this, linha.itemCardapioId(), linha.nome(), linha.precoUnitario(), linha.quantidade()));
		}

		this.subtotal = itens.stream().map(ItemPedido::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
		this.taxaEntrega = restaurante.getTaxaEntrega();
		this.total = subtotal.add(taxaEntrega);

		registrarStatus(StatusPedido.AGUARDANDO_PAGAMENTO, agora);
	}

	/** Toda mudança de status passa por aqui, então nenhuma escapa da máquina de estados. */
	public void mudarStatus(StatusPedido novo, Instant agora) {
		if (!status.podeIrPara(novo)) {
			throw new TransicaoStatusInvalidaException(status, novo);
		}
		registrarStatus(novo, agora);
	}

	private void registrarStatus(StatusPedido novo, Instant agora) {
		this.status = novo;
		this.atualizadoEm = agora;
		HistoricoStatusPedido registro = new HistoricoStatusPedido(this, novo, agora);
		historico.add(registro);
		mudancasNaoPublicadas.add(registro);
	}

	/**
	 * Chamado pelo Spring Data no save(). Os eventos são montados aqui, e não em
	 * registrarStatus, porque um pedido novo só tem id depois de persistido.
	 */
	@DomainEvents
	Collection<PedidoStatusAlterado> eventosDeDominio() {
		return mudancasNaoPublicadas.stream()
				.map(registro -> new PedidoStatusAlterado(
						id, clienteId, restaurante.getId(), restaurante.getUsuarioId(),
						registro.getStatus(), registro.getOcorridoEm()))
				.toList();
	}

	@AfterDomainEventPublication
	void limparEventos() {
		mudancasNaoPublicadas.clear();
	}

	public boolean pertenceAoCliente(Long clienteId) {
		return this.clienteId.equals(clienteId);
	}

	public Long getId() {
		return id;
	}

	public Long getClienteId() {
		return clienteId;
	}

	public Restaurante getRestaurante() {
		return restaurante;
	}

	public StatusPedido getStatus() {
		return status;
	}

	public BigDecimal getSubtotal() {
		return subtotal;
	}

	public BigDecimal getTaxaEntrega() {
		return taxaEntrega;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public String getEnderecoEntrega() {
		return enderecoEntrega;
	}

	public String getObservacao() {
		return observacao;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}

	public Instant getAtualizadoEm() {
		return atualizadoEm;
	}

	public List<ItemPedido> getItens() {
		return List.copyOf(itens);
	}

	public List<HistoricoStatusPedido> getHistorico() {
		return List.copyOf(historico);
	}
}
