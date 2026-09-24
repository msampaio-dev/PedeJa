package com.pedeja.pagamento.repository;

import java.sql.Timestamp;
import java.time.Instant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WebhookEventoRepository {

	private final JdbcTemplate jdbcTemplate;

	public WebhookEventoRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Devolve true só na primeira vez que o evento chega.
	 *
	 * ON CONFLICT DO NOTHING em vez de deixar a chave primária estourar: uma
	 * exceção de banco marcaria a transação para rollback. Se duas entregas do
	 * mesmo evento chegarem juntas, a segunda espera a primeira terminar e então
	 * recebe 0 linhas inseridas.
	 */
	public boolean registrarSeNovo(String eventoId, Instant recebidoEm) {
		int inseridos = jdbcTemplate.update(
				"INSERT INTO webhook_eventos_processados (evento_id, recebido_em) VALUES (?, ?) ON CONFLICT DO NOTHING",
				eventoId,
				Timestamp.from(recebidoEm));
		return inseridos == 1;
	}
}
