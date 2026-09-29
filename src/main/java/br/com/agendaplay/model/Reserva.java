package br.com.agendaplay.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record Reserva(
        long id,
        long idCliente,
        long idQuadra,
        LocalDate data,
        LocalTime horaInicio,
        LocalTime horaFim,
        String situacao,
        BigDecimal valorTotal,
        String nomeQuadra,
        String nomeCliente,
        int toleranciaMinutos) {}
