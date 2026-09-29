-- A baixa preserva o histórico e o descanso após o horário reservado.
ALTER TABLE reserva DROP CONSTRAINT reserva_id_quadra_tsrange_excl;
ALTER TABLE reserva DROP CONSTRAINT reserva_intervalo_protegido;
ALTER TABLE reserva ADD CONSTRAINT reserva_intervalo_protegido
 EXCLUDE USING gist (
 id_quadra WITH =,
 tsrange(data + hora_inicio, data + hora_fim + tolerancia_minutos * interval '1 minute', '[)') WITH &&
 ) WHERE (situacao IN ('CONFIRMADA','CONCLUIDA'));
