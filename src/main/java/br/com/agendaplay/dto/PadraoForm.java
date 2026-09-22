package br.com.agendaplay.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public class PadraoForm {
    @NotEmpty(message = "Selecione os dias da semana.")
    private List<@Min(1) @Max(7) Integer> dias = new ArrayList<>();

    @NotNull private LocalTime horaInicio;
    @NotNull private LocalTime horaFim;

    @Min(1)
    @Max(1440)
    private int duracaoMinutos = 60;

    @Min(0)
    @Max(1440)
    private int toleranciaMinutos = 10;

    @NotNull
    @DecimalMin("0")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal valorHora;

    public List<Integer> getDias() {
        return dias;
    }

    public void setDias(List<Integer> v) {
        dias = v;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime v) {
        horaInicio = v;
    }

    public LocalTime getHoraFim() {
        return horaFim;
    }

    public void setHoraFim(LocalTime v) {
        horaFim = v;
    }

    public int getDuracaoMinutos() {
        return duracaoMinutos;
    }

    public void setDuracaoMinutos(int v) {
        duracaoMinutos = v;
    }

    public int getToleranciaMinutos() {
        return toleranciaMinutos;
    }

    public void setToleranciaMinutos(int v) {
        toleranciaMinutos = v;
    }

    public BigDecimal getValorHora() {
        return valorHora;
    }

    public void setValorHora(BigDecimal v) {
        valorHora = v;
    }
}
