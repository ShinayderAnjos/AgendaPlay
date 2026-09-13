package br.com.agendaplay.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record HorarioLivre(LocalDate data, LocalTime horaInicio, LocalTime horaFim) {}
