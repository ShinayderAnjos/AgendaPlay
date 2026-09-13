package br.com.agendaplay.dto;

import jakarta.validation.constraints.*;

import java.time.*;

public class PeriodoForm {
    @NotNull(message = "Informe a data.")
    @org.springframework.format.annotation.DateTimeFormat(
            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private LocalDate data;

    @NotNull(message = "Informe o horário inicial.")
    @org.springframework.format.annotation.DateTimeFormat(
            iso = org.springframework.format.annotation.DateTimeFormat.ISO.TIME)
    private LocalTime horaInicio;

    @NotNull(message = "Informe o horário final.")
    @org.springframework.format.annotation.DateTimeFormat(
            iso = org.springframework.format.annotation.DateTimeFormat.ISO.TIME)
    private LocalTime horaFim;

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFim() {
        return horaFim;
    }

    public void setHoraFim(LocalTime horaFim) {
        this.horaFim = horaFim;
    }
}
