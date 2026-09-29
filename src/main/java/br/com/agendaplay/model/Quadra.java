package br.com.agendaplay.model;

import java.math.BigDecimal;

public record Quadra(
        long id,
        long idProprietario,
        String nome,
        String modalidade,
        String localizacao,
        BigDecimal valorHora,
        String situacao,
        long idEstabelecimento,
        int toleranciaMinutos,
        BigDecimal latitude,
        BigDecimal longitude) {}
