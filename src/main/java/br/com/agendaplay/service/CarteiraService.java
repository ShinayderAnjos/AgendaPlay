package br.com.agendaplay.service;

import br.com.agendaplay.exception.RegraNegocioException;
import br.com.agendaplay.model.Perfil;
import br.com.agendaplay.security.UsuarioAutenticado;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class CarteiraService {
    private final JdbcClient jdbc;
    private final BigDecimal percentual;
    private final BigDecimal minimo;
    private final boolean ativo;
    public CarteiraService(JdbcClient jdbc,
            @Value("${agendaplay.cashback.percentual:5}") BigDecimal percentual,
            @Value("${agendaplay.cashback.valor-minimo:0}") BigDecimal minimo,
            @Value("${agendaplay.cashback.ativo:true}") boolean ativo) {
        if (percentual.signum()<0 || percentual.compareTo(new BigDecimal("100"))>0 || minimo.signum()<0)
            throw new IllegalArgumentException("Configuração de cashback inválida.");
        this.jdbc=jdbc; this.percentual=percentual; this.minimo=minimo; this.ativo=ativo;
    }
    private void cliente(UsuarioAutenticado u) {
        if(u==null || u.getPerfil()!=Perfil.CLIENTE) throw new AccessDeniedException("Acesso exclusivo do cliente.");
    }
    private BigDecimal bloquear(long cliente) {
        jdbc.sql("INSERT INTO carteira(id_cliente) VALUES (?) ON CONFLICT DO NOTHING").param(cliente).update();
        return jdbc.sql("SELECT saldo FROM carteira WHERE id_cliente=? FOR UPDATE").param(cliente).query(BigDecimal.class).single();
    }
    public BigDecimal saldo(UsuarioAutenticado u) {
        cliente(u);
        return jdbc.sql("SELECT saldo FROM carteira WHERE id_cliente=?").param(u.getId())
                .query(BigDecimal.class).optional().orElse(BigDecimal.ZERO);
    }
    public List<Movimento> historico(UsuarioAutenticado u) {
        cliente(u);
        return jdbc.sql("SELECT id,tipo,valor,id_reserva,criado_em FROM movimento_carteira WHERE id_cliente=? ORDER BY criado_em DESC,id DESC")
                .param(u.getId()).query(Movimento.class).list();
    }
    @Transactional
    public void recarregar(BigDecimal valor,UUID chave,UsuarioAutenticado u) {
        cliente(u); CaixaService.validarValor(valor,false);
        if(chave==null) throw new RegraNegocioException("Identificador da recarga obrigatório.");
        var anterior=jdbc.sql("SELECT id_cliente,tipo,valor FROM movimento_carteira WHERE chave=?")
                .param(chave).query(Operacao.class).optional();
        if(anterior.isPresent()) { validarReenvio(anterior.get(),u.getId(),"RECARGA",valor); return; }
        bloquear(u.getId());
        anterior=jdbc.sql("SELECT id_cliente,tipo,valor FROM movimento_carteira WHERE chave=?")
                .param(chave).query(Operacao.class).optional();
        if(anterior.isPresent()) { validarReenvio(anterior.get(),u.getId(),"RECARGA",valor); return; }
        mover(u.getId(),null,"RECARGA",valor,chave);
    }
    private void validarReenvio(Operacao o,long cliente,String tipo,BigDecimal valor) {
        if(o.idCliente()!=cliente || !o.tipo().equals(tipo) || o.valor().compareTo(valor)!=0)
            throw new RegraNegocioException("Identificador já utilizado em outra operação.");
    }
    private void mover(long cliente,Long reserva,String tipo,BigDecimal valor,UUID chave) {
        jdbc.sql("UPDATE carteira SET saldo=saldo+? WHERE id_cliente=?").params(valor,cliente).update();
        jdbc.sql("INSERT INTO movimento_carteira(id_cliente,id_reserva,tipo,valor,chave) VALUES (?,?,?,?,?)")
                .params(cliente,reserva,tipo,valor,chave).update();
    }
    @Transactional
    public void pagar(long reserva,UsuarioAutenticado u) {
        cliente(u);
        var r=jdbc.sql("SELECT id_cliente,situacao,valor_total FROM reserva WHERE id=? FOR UPDATE")
                .param(reserva).query(ReservaPagamento.class).optional()
                .orElseThrow(()->new RegraNegocioException("Reserva não encontrada."));
        if(r.idCliente()!=u.getId()) throw new AccessDeniedException("Reserva de outro cliente.");
        if(!r.situacao().equals("CONFIRMADA")) throw new RegraNegocioException("Reserva não está ativa.");
        if(jdbc.sql("SELECT count(*) FROM recebimento WHERE id_reserva=? AND forma='CARTEIRA'")
                .param(reserva).query(Long.class).single()>0) return;
        var restante=jdbc.sql("SELECT COALESCE(sum(valor),0) FROM recebimento WHERE id_reserva=?")
                .param(reserva).query(BigDecimal.class).single();
        BigDecimal valor=r.valorTotal().subtract(restante);
        if(valor.signum()<=0) throw new RegraNegocioException("Reserva já quitada.");
        if(bloquear(u.getId()).compareTo(valor)<0) throw new RegraNegocioException("Saldo insuficiente na carteira.");
        mover(u.getId(),reserva,"PAGAMENTO",valor.negate(),null);
        jdbc.sql("INSERT INTO recebimento(id_reserva,forma,valor,chave) VALUES (?,'CARTEIRA',?,?)")
                .params(reserva,valor,UUID.randomUUID()).update();
        BigDecimal bonus=ativo && valor.compareTo(minimo)>=0
                ? valor.multiply(percentual).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP) : BigDecimal.ZERO;
        if(bonus.signum()>0) mover(u.getId(),reserva,"CASHBACK",bonus,null);
    }
    // Chamado depois de bloquear a reserva no cancelamento, na mesma transação.
    public void estornarCancelamento(long reserva,long cliente) {
        var pago=jdbc.sql("SELECT valor FROM recebimento WHERE id_reserva=? AND forma='CARTEIRA' AND estornado_em IS NULL")
                .param(reserva).query(BigDecimal.class).optional();
        if(pago.isEmpty()) return;
        bloquear(cliente);
        BigDecimal bonus=jdbc.sql("SELECT COALESCE(sum(valor),0) FROM movimento_carteira WHERE id_reserva=? AND tipo='CASHBACK'")
                .param(reserva).query(BigDecimal.class).single();
        mover(cliente,reserva,"ESTORNO",pago.get(),null);
        if(bonus.signum()>0) mover(cliente,reserva,"REVERSAO_CASHBACK",bonus.negate(),null);
        jdbc.sql("UPDATE recebimento SET estornado_em=current_timestamp WHERE id_reserva=? AND forma='CARTEIRA' AND estornado_em IS NULL")
                .param(reserva).update();
    }
    public record Movimento(long id,String tipo,BigDecimal valor,Long idReserva,OffsetDateTime criadoEm) {}
    private record Operacao(long idCliente,String tipo,BigDecimal valor) {}
    private record ReservaPagamento(long idCliente,String situacao,BigDecimal valorTotal) {}
}
