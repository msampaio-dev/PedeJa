CREATE TABLE pagamentos (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    pedido_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    valor NUMERIC(10, 2) NOT NULL,
    gateway_cobranca_id VARCHAR(100) NOT NULL,
    pix_copia_e_cola VARCHAR(500) NOT NULL,
    expira_em TIMESTAMPTZ NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL,
    atualizado_em TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_pagamentos PRIMARY KEY (id),
    CONSTRAINT fk_pagamentos_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos (id),
    CONSTRAINT uk_pagamentos_gateway_cobranca UNIQUE (gateway_cobranca_id),
    CONSTRAINT ck_pagamentos_status CHECK (status IN ('PENDENTE', 'APROVADO', 'RECUSADO', 'EXPIRADO')),
    CONSTRAINT ck_pagamentos_valor CHECK (valor > 0)
);

-- Um pedido pode ter várias tentativas (uma recusada, outra aprovada), mas
-- nunca duas cobranças pendentes ao mesmo tempo.
CREATE UNIQUE INDEX uk_pagamentos_pedido_pendente ON pagamentos (pedido_id) WHERE status = 'PENDENTE';

-- Cada notificação do gateway tem um id. Gravar o id antes de processar é o
-- que torna o webhook idempotente: a mesma notificação entregue duas vezes
-- esbarra na chave primária e é ignorada.
CREATE TABLE webhook_eventos_processados (
    evento_id VARCHAR(100) NOT NULL,
    recebido_em TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_webhook_eventos_processados PRIMARY KEY (evento_id)
);
