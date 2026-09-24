package com.pedeja.outbox;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedeja.mensageria.MensagemPedido;
import com.pedeja.pedido.entity.PedidoStatusAlterado;

/**
 * Transforma cada evento de domínio do pedido numa linha do outbox.
 *
 * Roda de forma síncrona, dentro da transação de quem salvou o pedido
 * (MANDATORY falha se não houver uma). Se a transação for desfeita, o evento
 * some junto; se for confirmada, o evento está garantido no banco, mesmo que o
 * RabbitMQ esteja fora do ar neste momento.
 */
@Component
public class EscritorOutbox {

	private final OutboxRepository outboxRepository;
	private final ObjectMapper objectMapper;

	public EscritorOutbox(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
		this.outboxRepository = outboxRepository;
		this.objectMapper = objectMapper;
	}

	@EventListener
	@Transactional(propagation = Propagation.MANDATORY)
	public void registrar(PedidoStatusAlterado evento) throws JsonProcessingException {
		MensagemPedido mensagem = MensagemPedido.de(evento);

		outboxRepository.save(new OutboxEvento(
				mensagem.eventoId(),
				PedidoStatusAlterado.class.getSimpleName(),
				mensagem.routingKey(),
				mensagem.pedidoId(),
				objectMapper.writeValueAsString(mensagem),
				evento.ocorridoEm()));
	}
}
