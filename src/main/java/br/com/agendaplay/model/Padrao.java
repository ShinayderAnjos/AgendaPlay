package br.com.agendaplay.model;

import java.math.BigDecimal;
import java.time.LocalTime;

public record Padrao(
        long id,
        long idQuadra,
        String dias,
        LocalTime horaInicio,
        LocalTime horaFim,
        int duracaoMinutos,
        int toleranciaMinutos,
        BigDecimal valorHora,
        boolean ativo) {}
