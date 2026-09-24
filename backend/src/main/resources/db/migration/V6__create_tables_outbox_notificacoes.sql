-- Transactional outbox. O evento é gravado na mesma transação que muda o
-- pedido; um processo separado lê esta tabela e publica no RabbitMQ. Assim
-- não existe pedido alterado sem evento, nem evento de mudança que não aconteceu.
CREATE TABLE outbox_eventos (
    id UUID NOT NULL,
    tipo VARCHAR(60) NOT NULL,
    routing_key VARCHAR(100) NOT NULL,
    pedido_id BIGINT NOT NULL,
    payload JSONB NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL,
    publicado_em TIMESTAMPTZ,
    tentativas INTEGER NOT NULL DEFAULT 0,
    ultimo_erro VARCHAR(500),

    CONSTRAINT pk_outbox_eventos PRIMARY KEY (id)
);

-- O publicador só procura o que falta publicar. O índice parcial fica pequeno
-- mesmo com milhões de eventos antigos já publicados.
CREATE INDEX ix_outbox_eventos_pendentes ON outbox_eventos (criado_em) WHERE publicado_em IS NULL;

CREATE TABLE notificacoes (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    evento_id UUID NOT NULL,
    usuario_id BIGINT NOT NULL,
    pedido_id BIGINT NOT NULL,
    mensagem VARCHAR(300) NOT NULL,
    lida BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_notificacoes PRIMARY KEY (id),
    CONSTRAINT fk_notificacoes_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_notificacoes_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos (id),
    -- O RabbitMQ entrega "pelo menos uma vez". A mesma mensagem processada de
    -- novo esbarra aqui e não gera notificação repetida.
    CONSTRAINT uk_notificacoes_evento_usuario UNIQUE (evento_id, usuario_id)
);

CREATE INDEX ix_notificacoes_usuario_criado_em ON notificacoes (usuario_id, criado_em DESC);
