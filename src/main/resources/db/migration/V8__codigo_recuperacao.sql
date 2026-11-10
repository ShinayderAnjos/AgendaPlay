ALTER TABLE recuperacao_senha ADD COLUMN codigo_hash VARCHAR(100);
ALTER TABLE recuperacao_senha ADD COLUMN tentativas INTEGER NOT NULL DEFAULT 0 CHECK (tentativas BETWEEN 0 AND 5);
CREATE TABLE recuperacao_envio (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario BIGINT NOT NULL REFERENCES usuario(id),
    enviado_em TIMESTAMPTZ NOT NULL
);
CREATE INDEX recuperacao_envio_usuario ON recuperacao_envio(id_usuario, enviado_em);
