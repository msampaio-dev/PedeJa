CREATE TABLE usuarios (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    nome VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    senha_hash VARCHAR(100) NOT NULL,
    perfil VARCHAR(20) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_usuarios PRIMARY KEY (id),
    CONSTRAINT uk_usuarios_email UNIQUE (email),
    -- O service grava o e-mail em minusculas; a constraint garante isso mesmo
    -- para quem inserir direto no banco.
    CONSTRAINT ck_usuarios_email_minusculo CHECK (email = lower(email)),
    CONSTRAINT ck_usuarios_perfil CHECK (perfil IN ('CLIENTE', 'RESTAURANTE'))
);
