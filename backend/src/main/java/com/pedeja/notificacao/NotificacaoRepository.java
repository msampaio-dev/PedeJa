package com.pedeja.notificacao;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class NotificacaoRepository {

	private final JdbcTemplate jdbcTemplate;

	public NotificacaoRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public record Notificacao(Long id, Long pedidoId, String mensagem, boolean lida, Instant criadoEm) {
	}

	/** Mensagem repetida do RabbitMQ esbarra em uk_notificacoes_evento_usuario e não grava nada. */
	public void inserirSeNova(UUID eventoId, Long usuarioId, Long pedidoId, String mensagem, Instant criadoEm) {
		jdbcTemplate.update("""
				INSERT INTO notificacoes (evento_id, usuario_id, pedido_id, mensagem, criado_em)
				VALUES (?, ?, ?, ?, ?)
				ON CONFLICT DO NOTHING
				""", eventoId, usuarioId, pedidoId, mensagem, Timestamp.from(criadoEm));
	}

	public List<Notificacao> ultimas(Long usuarioId, int limite) {
		return jdbcTemplate.query("""
				SELECT id, pedido_id, mensagem, lida, criado_em
				FROM notificacoes
				WHERE usuario_id = ?
				ORDER BY criado_em DESC, id DESC
				LIMIT ?
				""",
				(linha, numero) -> new Notificacao(
						linha.getLong("id"),
						linha.getLong("pedido_id"),
						linha.getString("mensagem"),
						linha.getBoolean("lida"),
						linha.getTimestamp("criado_em").toInstant()),
				usuarioId, limite);
	}

	public void marcarTodasComoLidas(Long usuarioId) {
		jdbcTemplate.update("UPDATE notificacoes SET lida = TRUE WHERE usuario_id = ? AND lida = FALSE", usuarioId);
	}
}
