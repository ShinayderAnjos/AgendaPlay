package br.com.agendaplay.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record Disponibilidade(
        long id,
        long idQuadra,
        LocalDate data,
        LocalTime horaInicio,
        LocalTime horaFim,
        boolean ativo,
        Long idPadrao,
        java.math.BigDecimal valorHora,
        int toleranciaMinutos) {}
