CREATE TABLE itens_cardapio (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    restaurante_id BIGINT NOT NULL,
    nome VARCHAR(120) NOT NULL,
    descricao VARCHAR(500),
    preco NUMERIC(10, 2) NOT NULL,
    disponivel BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_itens_cardapio PRIMARY KEY (id),
    CONSTRAINT fk_itens_cardapio_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurantes (id),
    CONSTRAINT ck_itens_cardapio_preco CHECK (preco > 0)
);

-- Dois itens com o mesmo nome no mesmo cardápio confundem o cliente. O índice
-- compara sem diferenciar maiúsculas e também atende a busca por restaurante.
CREATE UNIQUE INDEX uk_itens_cardapio_restaurante_nome ON itens_cardapio (restaurante_id, lower(nome));
