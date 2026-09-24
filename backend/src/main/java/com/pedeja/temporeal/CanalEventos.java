package com.pedeja.temporeal;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Conexões SSE abertas nesta instância, por usuário. Um usuário pode ter várias
 * (duas abas, celular e computador), e todas recebem o evento.
 *
 * Fica em memória porque a conexão HTTP é desta instância. Com várias
 * instâncias, cada uma recebe todos os eventos do RabbitMQ (ver
 * ConsumidorTempoReal) e entrega só para quem está conectado nela.
 */
@Component
@Lazy(false)
public class CanalEventos {

	private static final Logger log = LoggerFactory.getLogger(CanalEventos.class);

	// O navegador reconecta sozinho quando o prazo acaba.
	private static final long TEMPO_MAXIMO_CONEXAO_MS = 30 * 60 * 1000L;

	private final Map<Long, List<SseEmitter>> conexoes = new ConcurrentHashMap<>();

	public SseEmitter conectar(Long usuarioId) {
		SseEmitter emitter = new SseEmitter(TEMPO_MAXIMO_CONEXAO_MS);
		conexoes.computeIfAbsent(usuarioId, id -> new CopyOnWriteArrayList<>()).add(emitter);

		Runnable remover = () -> remover(usuarioId, emitter);
		emitter.onCompletion(remover);
		emitter.onTimeout(remover);
		emitter.onError(erro -> remover.run());

		enviar(usuarioId, emitter, "conectado", Map.of());
		return emitter;
	}

	public void publicar(Long usuarioId, String nome, Object dados) {
		for (SseEmitter emitter : conexoes.getOrDefault(usuarioId, List.of())) {
			enviar(usuarioId, emitter, nome, dados);
		}
	}

	/**
	 * Proxies e balanceadores (o do Render incluso) derrubam conexão parada. Um
	 * comentário SSE a cada 25 segundos mantém a conexão viva e, de quebra,
	 * descobre as que o cliente já fechou.
	 */
	@Scheduled(fixedRate = 25_000)
	public void manterConexoesVivas() {
		conexoes.forEach((usuarioId, lista) -> lista.forEach(emitter -> {
			try {
				emitter.send(SseEmitter.event().comment("ping"));
			} catch (IOException | IllegalStateException e) {
				remover(usuarioId, emitter);
			}
		}));
	}

	public int conexoesAbertas() {
		return conexoes.values().stream().mapToInt(List::size).sum();
	}

	private void enviar(Long usuarioId, SseEmitter emitter, String nome, Object dados) {
		try {
			emitter.send(SseEmitter.event().name(nome).data(dados, MediaType.APPLICATION_JSON));
		} catch (IOException | IllegalStateException e) {
			log.debug("Conexão SSE do usuário {} fechada: {}", usuarioId, e.getMessage());
			remover(usuarioId, emitter);
		}
	}

	private void remover(Long usuarioId, SseEmitter emitter) {
		conexoes.computeIfPresent(usuarioId, (id, lista) -> {
			lista.remove(emitter);
			return lista.isEmpty() ? null : lista;
		});
	}
}
