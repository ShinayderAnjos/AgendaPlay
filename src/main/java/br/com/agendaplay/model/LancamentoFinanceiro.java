package br.com.agendaplay.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record LancamentoFinanceiro(
        long idReserva,
        LocalDate data,
        LocalTime horaInicio,
        String nomeQuadra,
        String nomeCliente,
        String nomeEstabelecimento,
        long idEstabelecimento,
        String situacao,
        BigDecimal valorTotal,
        BigDecimal valorRecebido,
        BigDecimal valorEstornado,
        String forma) {
    public BigDecimal saldoRestante() {
        return situacao.equals("CANCELADA") ? BigDecimal.ZERO : valorTotal.subtract(valorRecebido);
    }
    public BigDecimal estornoPendente() {
        return situacao.equals("CANCELADA") ? valorRecebido.subtract(valorEstornado) : BigDecimal.ZERO;
    }
    public String estadoFinanceiro() {
        if (situacao.equals("CANCELADA")) {
            if (estornoPendente().signum() > 0) return "ESTORNO PENDENTE";
            return valorRecebido.signum() > 0 ? "ESTORNADO" : "CANCELADA";
        }
        if (saldoRestante().signum() == 0) return "RECEBIDO";
        return valorRecebido.signum() > 0 ? "PARCIAL" : "PENDENTE";
    }
    public BigDecimal entradaMetade() {
        return saldoRestante().min(valorTotal.divide(new BigDecimal("2"), 2, java.math.RoundingMode.HALF_UP));
    }
    public String chaveOperacao() { return java.util.UUID.randomUUID().toString(); }
}
