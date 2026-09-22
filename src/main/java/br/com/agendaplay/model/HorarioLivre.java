package br.com.agendaplay.model;

import java.math.BigDecimal;
import java.time.*;

public record HorarioLivre(
        LocalDate data,
        LocalTime horaInicio,
        LocalTime horaFim,
        BigDecimal valorHora,
        int toleranciaMinutos) {
    public HorarioLivre(LocalDate d, LocalTime i, LocalTime f) {
        this(d, i, f, null, 0);
    }
}
