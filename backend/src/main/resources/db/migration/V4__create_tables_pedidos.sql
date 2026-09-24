CREATE TABLE pedidos (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    cliente_id BIGINT NOT NULL,
    restaurante_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    subtotal NUMERIC(10, 2) NOT NULL,
    taxa_entrega NUMERIC(10, 2) NOT NULL,
    total NUMERIC(10, 2) NOT NULL,
    endereco_entrega VARCHAR(300) NOT NULL,
    observacao VARCHAR(300),
    chave_idempotencia VARCHAR(100),
    criado_em TIMESTAMPTZ NOT NULL,
    atualizado_em TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_pedidos PRIMARY KEY (id),
    CONSTRAINT fk_pedidos_cliente FOREIGN KEY (cliente_id) REFERENCES usuarios (id),
    CONSTRAINT fk_pedidos_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurantes (id),
    CONSTRAINT ck_pedidos_status CHECK (status IN (
        'AGUARDANDO_PAGAMENTO', 'PAGO', 'ACEITO', 'EM_PREPARO',
        'SAIU_PARA_ENTREGA', 'ENTREGUE', 'CANCELADO', 'RECUSADO')),
    CONSTRAINT ck_pedidos_valores CHECK (subtotal > 0 AND taxa_entrega >= 0),
    -- O banco confere a conta: um bug no cálculo vira erro, não cobrança errada.
    CONSTRAINT ck_pedidos_total CHECK (total = subtotal + taxa_entrega),
    -- A mesma chave do mesmo cliente representa o mesmo pedido. Um clique duplo
    -- ou uma nova tentativa depois de falha de rede não cria um segundo pedido.
    CONSTRAINT uk_pedidos_cliente_chave UNIQUE (cliente_id, chave_idempotencia)
);

CREATE INDEX ix_pedidos_cliente_criado_em ON pedidos (cliente_id, criado_em DESC);
CREATE INDEX ix_pedidos_restaurante_status ON pedidos (restaurante_id, status);

CREATE TABLE itens_pedido (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    pedido_id BIGINT NOT NULL,
    item_cardapio_id BIGINT NOT NULL,
    -- Nome e preço copiados do cardápio no momento da compra. Se o restaurante
    -- mudar o preço amanhã, o pedido de hoje continua com o valor que foi pago.
    nome VARCHAR(120) NOT NULL,
    preco_unitario NUMERIC(10, 2) NOT NULL,
    quantidade INTEGER NOT NULL,
    subtotal NUMERIC(10, 2) NOT NULL,

    CONSTRAINT pk_itens_pedido PRIMARY KEY (id),
    CONSTRAINT fk_itens_pedido_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos (id),
    CONSTRAINT fk_itens_pedido_item_cardapio FOREIGN KEY (item_cardapio_id) REFERENCES itens_cardapio (id),
    CONSTRAINT ck_itens_pedido_quantidade CHECK (quantidade BETWEEN 1 AND 50),
    CONSTRAINT ck_itens_pedido_subtotal CHECK (subtotal = preco_unitario * quantidade)
);

CREATE INDEX ix_itens_pedido_pedido ON itens_pedido (pedido_id);

CREATE TABLE historico_status_pedido (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    pedido_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    ocorrido_em TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_historico_status_pedido PRIMARY KEY (id),
    CONSTRAINT fk_historico_status_pedido_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos (id)
);

CREATE INDEX ix_historico_status_pedido_pedido ON historico_status_pedido (pedido_id);
