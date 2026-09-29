-- Também protege o intervalo de descanso em gravações concorrentes no banco.
-- Reservas anteriores à Sprint 3 têm tolerância zero, preservando sua validade.
ALTER TABLE reserva ADD CONSTRAINT reserva_intervalo_protegido
 EXCLUDE USING gist (
  id_quadra WITH =,
  tsrange(data + hora_inicio, data + hora_fim + tolerancia_minutos * interval '1 minute', '[)') WITH &&
 ) WHERE (situacao = 'CONFIRMADA');
