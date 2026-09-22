package br.com.agendaplay.model;

import java.math.BigDecimal;

public record Estabelecimento(
        long id,
        long idProprietario,
        String nome,
        String localizacao,
        BigDecimal latitude,
        BigDecimal longitude) {}
