ALTER TABLE usuario ALTER COLUMN cpf TYPE VARCHAR(14);
ALTER TABLE usuario DROP CONSTRAINT usuario_cpf_check;
ALTER TABLE usuario ADD CONSTRAINT documento_formato CHECK (cpf ~ '^([0-9]{11}|[A-Z0-9]{12}[0-9]{2})$');
-- Contas legadas são preservadas; a restrição vale para novos registros.
ALTER TABLE usuario ADD CONSTRAINT documento_obrigatorio CHECK (cpf IS NOT NULL) NOT VALID;
CREATE TABLE estabelecimento (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_proprietario BIGINT NOT NULL REFERENCES usuario(id), nome VARCHAR(120) NOT NULL,
 localizacao VARCHAR(240) NOT NULL,
 latitude NUMERIC(10,7) CHECK(latitude BETWEEN -90 AND 90),
 longitude NUMERIC(10,7) CHECK(longitude BETWEEN -180 AND 180),
 UNIQUE(id,id_proprietario), CHECK((latitude IS NULL) = (longitude IS NULL))
);
INSERT INTO estabelecimento(id_proprietario,nome,localizacao)
 SELECT id_proprietario,'Estabelecimento principal',min(localizacao) FROM quadra GROUP BY id_proprietario;
ALTER TABLE quadra ADD COLUMN id_estabelecimento BIGINT;
UPDATE quadra q SET id_estabelecimento=e.id FROM estabelecimento e WHERE e.id_proprietario=q.id_proprietario;
ALTER TABLE quadra ALTER COLUMN id_estabelecimento SET NOT NULL;
ALTER TABLE quadra ADD FOREIGN KEY(id_estabelecimento,id_proprietario) REFERENCES estabelecimento(id,id_proprietario);
ALTER TABLE quadra ADD COLUMN tolerancia_minutos INTEGER NOT NULL DEFAULT 10 CHECK(tolerancia_minutos BETWEEN 0 AND 1440);
ALTER TABLE quadra ADD COLUMN latitude NUMERIC(10,7) CHECK(latitude BETWEEN -90 AND 90);
ALTER TABLE quadra ADD COLUMN longitude NUMERIC(10,7) CHECK(longitude BETWEEN -180 AND 180);
ALTER TABLE quadra ADD CHECK((latitude IS NULL) = (longitude IS NULL));
CREATE TABLE padrao_disponibilidade (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, id_quadra BIGINT NOT NULL REFERENCES quadra(id),
 dias VARCHAR(13) NOT NULL, hora_inicio TIME NOT NULL, hora_fim TIME NOT NULL CHECK(hora_fim > hora_inicio),
 duracao_minutos INTEGER NOT NULL CHECK(duracao_minutos BETWEEN 1 AND 1440),
 tolerancia_minutos INTEGER NOT NULL CHECK(tolerancia_minutos BETWEEN 0 AND 1440),
 valor_hora NUMERIC(10,2) NOT NULL CHECK(valor_hora >= 0), ativo BOOLEAN NOT NULL DEFAULT true
);
ALTER TABLE disponibilidade ADD COLUMN id_padrao BIGINT REFERENCES padrao_disponibilidade(id);
ALTER TABLE disponibilidade ADD COLUMN valor_hora NUMERIC(10,2) CHECK(valor_hora >= 0);
ALTER TABLE disponibilidade ADD COLUMN tolerancia_minutos INTEGER NOT NULL DEFAULT 10 CHECK(tolerancia_minutos BETWEEN 0 AND 1440);
CREATE UNIQUE INDEX disponibilidade_padrao_horario ON disponibilidade(id_padrao,data,hora_inicio);
ALTER TABLE reserva ADD COLUMN tolerancia_minutos INTEGER NOT NULL DEFAULT 0 CHECK(tolerancia_minutos BETWEEN 0 AND 1440);
CREATE TABLE notificacao (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, id_usuario BIGINT NOT NULL REFERENCES usuario(id),
 id_reserva BIGINT NOT NULL REFERENCES reserva(id), mensagem VARCHAR(500) NOT NULL,
 lida BOOLEAN NOT NULL DEFAULT false, criada_em TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
 UNIQUE(id_usuario,id_reserva)
);
CREATE INDEX notificacao_pendente ON notificacao(id_usuario) WHERE NOT lida;
