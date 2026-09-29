ALTER TABLE reserva DROP CONSTRAINT reserva_situacao_check;
ALTER TABLE reserva ADD CONSTRAINT reserva_situacao_check CHECK (situacao IN ('CONFIRMADA','CANCELADA','CONCLUIDA'));
ALTER TABLE reserva ADD COLUMN concluida_em TIMESTAMPTZ;
CREATE TABLE avaliacao (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_reserva BIGINT NOT NULL UNIQUE REFERENCES reserva(id),
 nota INTEGER NOT NULL CHECK (nota BETWEEN 1 AND 5),
 comentario VARCHAR(1000) NOT NULL DEFAULT '',
 criado_em TIMESTAMPTZ NOT NULL DEFAULT current_timestamp
);
CREATE TABLE foto_quadra (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_quadra BIGINT NOT NULL REFERENCES quadra(id),
 conteudo BYTEA NOT NULL,
 capa BOOLEAN NOT NULL DEFAULT false,
 criado_em TIMESTAMPTZ NOT NULL DEFAULT current_timestamp
);
CREATE UNIQUE INDEX foto_capa_unica ON foto_quadra(id_quadra) WHERE capa;
CREATE INDEX foto_quadra_indice ON foto_quadra(id_quadra);
CREATE TABLE recuperacao_senha (
 token_hash VARCHAR(64) PRIMARY KEY,
 id_usuario BIGINT NOT NULL REFERENCES usuario(id),
 expira_em TIMESTAMPTZ NOT NULL,
 criado_em TIMESTAMPTZ NOT NULL DEFAULT current_timestamp
);
CREATE INDEX recuperacao_usuario ON recuperacao_senha(id_usuario);
