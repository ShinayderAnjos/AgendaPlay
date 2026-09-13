CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA public;

CREATE TABLE usuario (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE CHECK (email = lower(trim(email))),
    cpf VARCHAR(11) UNIQUE CHECK (cpf ~ '^[0-9]{11}$'),
    telefone VARCHAR(20),
    senha_hash VARCHAR(100) NOT NULL,
    perfil VARCHAR(20) NOT NULL CHECK (perfil IN ('CLIENTE', 'PROPRIETARIO')),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    CHECK (perfil <> 'CLIENTE' OR cpf IS NOT NULL),
    UNIQUE (id, perfil)
);

CREATE TABLE quadra (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_proprietario BIGINT NOT NULL,
    perfil_proprietario VARCHAR(20) NOT NULL DEFAULT 'PROPRIETARIO' CHECK (perfil_proprietario = 'PROPRIETARIO'),
    nome VARCHAR(120) NOT NULL,
    modalidade VARCHAR(60) NOT NULL,
    localizacao VARCHAR(240) NOT NULL,
    valor_hora NUMERIC(10,2) NOT NULL CHECK (valor_hora >= 0),
    situacao VARCHAR(10) NOT NULL CHECK (situacao IN ('ATIVA', 'INATIVA')),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    FOREIGN KEY (id_proprietario, perfil_proprietario) REFERENCES usuario(id, perfil)
);

CREATE TABLE disponibilidade (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_quadra BIGINT NOT NULL REFERENCES quadra(id),
    data DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fim TIME NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT true,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    CHECK (hora_fim > hora_inicio),
    EXCLUDE USING gist (
        id_quadra WITH =,
        tsrange(data + hora_inicio, data + hora_fim, '[)') WITH &&
    ) WHERE (ativo)
);

CREATE TABLE reserva (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cliente BIGINT NOT NULL,
    perfil_cliente VARCHAR(20) NOT NULL DEFAULT 'CLIENTE' CHECK (perfil_cliente = 'CLIENTE'),
    id_quadra BIGINT NOT NULL REFERENCES quadra(id),
    data DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fim TIME NOT NULL,
    situacao VARCHAR(12) NOT NULL DEFAULT 'CONFIRMADA' CHECK (situacao IN ('CONFIRMADA', 'CANCELADA')),
    valor_total NUMERIC(12,2) NOT NULL CHECK (valor_total >= 0),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    FOREIGN KEY (id_cliente, perfil_cliente) REFERENCES usuario(id, perfil),
    CHECK (hora_fim > hora_inicio),
    -- [) permite que uma reserva comece exatamente quando outra termina.
    EXCLUDE USING gist (
        id_quadra WITH =,
        tsrange(data + hora_inicio, data + hora_fim, '[)') WITH &&
    ) WHERE (situacao = 'CONFIRMADA')
);

CREATE INDEX idx_quadra_proprietario ON quadra(id_proprietario);
CREATE INDEX idx_reserva_cliente_data ON reserva(id_cliente, data);
CREATE INDEX idx_reserva_quadra_data ON reserva(id_quadra, data);
