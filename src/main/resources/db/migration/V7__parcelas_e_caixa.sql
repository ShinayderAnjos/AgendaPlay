CREATE TABLE caixa (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_estabelecimento BIGINT NOT NULL REFERENCES estabelecimento(id),
    aberto_por BIGINT NOT NULL REFERENCES usuario(id),
    aberto_em TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    saldo_inicial NUMERIC(12,2) NOT NULL CHECK (saldo_inicial >= 0),
    fechado_em TIMESTAMPTZ,
    saldo_esperado NUMERIC(12,2),
    saldo_informado NUMERIC(12,2) CHECK (saldo_informado >= 0),
    observacao VARCHAR(500) NOT NULL DEFAULT '',
    CHECK ((fechado_em IS NULL AND saldo_esperado IS NULL AND saldo_informado IS NULL)
        OR (fechado_em IS NOT NULL AND fechado_em >= aberto_em AND saldo_esperado IS NOT NULL AND saldo_informado IS NOT NULL))
);
CREATE UNIQUE INDEX caixa_aberto_unico ON caixa(id_estabelecimento) WHERE fechado_em IS NULL;
-- Converte os recebimentos integrais anteriores sem alterar valores ou datas.
ALTER TABLE recebimento DROP CONSTRAINT recebimento_pkey;
ALTER TABLE recebimento ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;
ALTER TABLE recebimento ADD COLUMN valor NUMERIC(12,2);
UPDATE recebimento p SET valor = r.valor_total FROM reserva r WHERE r.id = p.id_reserva;
ALTER TABLE recebimento ALTER COLUMN valor SET NOT NULL;
ALTER TABLE recebimento ADD CHECK (valor >= 0);
ALTER TABLE recebimento ADD COLUMN id_caixa BIGINT REFERENCES caixa(id);
ALTER TABLE recebimento ADD COLUMN id_caixa_estorno BIGINT REFERENCES caixa(id);
ALTER TABLE recebimento ADD COLUMN chave UUID UNIQUE;
CREATE INDEX recebimento_reserva ON recebimento(id_reserva);
CREATE INDEX recebimento_caixa ON recebimento(id_caixa);
CREATE INDEX recebimento_caixa_estorno ON recebimento(id_caixa_estorno);
