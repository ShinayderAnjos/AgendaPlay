CREATE TABLE recebimento (
    id_reserva BIGINT PRIMARY KEY REFERENCES reserva(id),
    forma VARCHAR(20) NOT NULL CHECK (forma IN ('PIX', 'DINHEIRO', 'CARTAO', 'TRANSFERENCIA', 'OUTRO')),
    recebido_em TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    estornado_em TIMESTAMPTZ,
    CHECK (estornado_em IS NULL OR estornado_em >= recebido_em)
);
