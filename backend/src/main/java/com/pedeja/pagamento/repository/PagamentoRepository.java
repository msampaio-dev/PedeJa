package com.pedeja.pagamento.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pedeja.pagamento.entity.Pagamento;
import com.pedeja.pagamento.entity.StatusPagamento;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

	Optional<Pagamento> findByGatewayCobrancaId(String gatewayCobrancaId);

	Optional<Pagamento> findByPedidoIdAndStatus(Long pedidoId, StatusPagamento status);

	Optional<Pagamento> findFirstByPedidoIdOrderByIdDesc(Long pedidoId);
}
