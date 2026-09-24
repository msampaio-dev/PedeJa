package com.pedeja.pedido.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pedeja.pedido.entity.Pedido;
import com.pedeja.pedido.entity.StatusPedido;

import jakarta.persistence.LockModeType;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

	Optional<Pedido> findByClienteIdAndChaveIdempotencia(Long clienteId, String chaveIdempotencia);

	Optional<Pedido> findByIdAndClienteId(Long id, Long clienteId);

	Optional<Pedido> findByIdAndRestauranteId(Long id, Long restauranteId);

	// Traz o restaurante na mesma consulta: a lista mostra o nome dele em cada
	// linha, e sem isso seria uma consulta extra por pedido (N+1).
	@EntityGraph(attributePaths = "restaurante")
	Page<Pedido> findByClienteIdOrderByCriadoEmDesc(Long clienteId, Pageable pageable);

	// Mais antigo primeiro: é a fila da cozinha.
	List<Pedido> findTop100ByRestauranteIdAndStatusInOrderByCriadoEmAsc(Long restauranteId, Collection<StatusPedido> status);

	/**
	 * SELECT ... FOR UPDATE. Quem muda o status trava a linha do pedido até o fim
	 * da transação, e quem chegar junto espera. Assim o cancelamento do cliente e
	 * a confirmação do pagamento nunca decidem em cima de um status desatualizado.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Pedido p where p.id = :id")
	Optional<Pedido> buscarParaAtualizar(@Param("id") Long id);
}
