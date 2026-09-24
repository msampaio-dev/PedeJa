package com.pedeja.outbox;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxRepository extends JpaRepository<OutboxEvento, UUID> {

	/**
	 * FOR UPDATE SKIP LOCKED: com mais de uma instância da API, cada uma pega um
	 * lote diferente. Linha travada por outra instância é pulada, em vez de ser
	 * publicada duas vezes ou de deixar esta instância esperando.
	 */
	@Query(value = """
			SELECT * FROM outbox_eventos
			WHERE publicado_em IS NULL
			ORDER BY criado_em
			LIMIT :limite
			FOR UPDATE SKIP LOCKED
			""", nativeQuery = true)
	List<OutboxEvento> travarPendentes(@Param("limite") int limite);

	long countByPedidoId(Long pedidoId);
}
