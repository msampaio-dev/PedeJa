CREATE TABLE restaurantes (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    usuario_id BIGINT NOT NULL,
    nome VARCHAR(120) NOT NULL,
    descricao VARCHAR(500),
    categoria VARCHAR(20) NOT NULL,
    taxa_entrega NUMERIC(10, 2) NOT NULL,
    tempo_entrega_minutos INTEGER NOT NULL,
    aberto BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_restaurantes PRIMARY KEY (id),
    -- Uma conta de restaurante administra exatamente um restaurante.
    CONSTRAINT uk_restaurantes_usuario UNIQUE (usuario_id),
    CONSTRAINT fk_restaurantes_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT ck_restaurantes_categoria
        CHECK (categoria IN ('PIZZA', 'LANCHES', 'JAPONESA', 'BRASILEIRA', 'DOCES', 'SAUDAVEL')),
    CONSTRAINT ck_restaurantes_taxa_entrega CHECK (taxa_entrega >= 0),
    CONSTRAINT ck_restaurantes_tempo_entrega CHECK (tempo_entrega_minutos BETWEEN 5 AND 180)
);

CREATE INDEX ix_restaurantes_categoria ON restaurantes (categoria);
