CREATE TABLE carteira (
    id_cliente BIGINT PRIMARY KEY REFERENCES usuario(id),
    saldo NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (saldo >= 0)
);
CREATE TABLE movimento_carteira (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cliente BIGINT NOT NULL REFERENCES usuario(id),
    id_reserva BIGINT REFERENCES reserva(id),
    tipo VARCHAR(24) NOT NULL CHECK (tipo IN ('RECARGA','PAGAMENTO','CASHBACK','ESTORNO','REVERSAO_CASHBACK')),
    valor NUMERIC(12,2) NOT NULL CHECK (valor <> 0),
    chave UUID UNIQUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT current_timestamp
);
CREATE UNIQUE INDEX movimento_carteira_reserva_tipo ON movimento_carteira(id_reserva,tipo) WHERE id_reserva IS NOT NULL;
CREATE INDEX movimento_carteira_cliente_data ON movimento_carteira(id_cliente,criado_em DESC);
ALTER TABLE recebimento DROP CONSTRAINT IF EXISTS recebimento_forma_check;
ALTER TABLE recebimento ADD CONSTRAINT recebimento_forma_check CHECK (forma IN ('PIX','DINHEIRO','CARTAO','TRANSFERENCIA','OUTRO','CARTEIRA'));
CREATE UNIQUE INDEX recebimento_carteira_reserva ON recebimento(id_reserva) WHERE forma='CARTEIRA';
CREATE TABLE despesa (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_estabelecimento BIGINT NOT NULL REFERENCES estabelecimento(id),
    descricao VARCHAR(200) NOT NULL,
    valor NUMERIC(12,2) NOT NULL CHECK (valor > 0),
    data DATE NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT current_timestamp
);
CREATE INDEX despesa_estabelecimento_data ON despesa(id_estabelecimento,data DESC);
