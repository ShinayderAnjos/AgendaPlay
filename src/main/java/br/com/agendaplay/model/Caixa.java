package br.com.agendaplay.model;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
public record Caixa(long id, long idEstabelecimento, String nomeEstabelecimento,
        OffsetDateTime abertoEm, BigDecimal saldoInicial, OffsetDateTime fechadoEm,
        BigDecimal saldoEsperado, BigDecimal saldoInformado, String observacao) {
    public Caixa {
        var zona=java.time.ZoneId.of("America/Sao_Paulo");
        abertoEm=abertoEm.atZoneSameInstant(zona).toOffsetDateTime();
        if(fechadoEm!=null) fechadoEm=fechadoEm.atZoneSameInstant(zona).toOffsetDateTime();
    }
    public BigDecimal diferenca() {
        return saldoInformado == null ? null : saldoInformado.subtract(saldoEsperado);
    }
}
