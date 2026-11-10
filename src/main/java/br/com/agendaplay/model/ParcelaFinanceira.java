package br.com.agendaplay.model;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
public record ParcelaFinanceira(long id, long idReserva, BigDecimal valor, String forma,
        OffsetDateTime recebidoEm, OffsetDateTime estornadoEm, Long idCaixa, Long idCaixaEstorno) {
    public ParcelaFinanceira {
        var zona=java.time.ZoneId.of("America/Sao_Paulo");
        recebidoEm=recebidoEm.atZoneSameInstant(zona).toOffsetDateTime();
        if(estornadoEm!=null) estornadoEm=estornadoEm.atZoneSameInstant(zona).toOffsetDateTime();
    }
}
