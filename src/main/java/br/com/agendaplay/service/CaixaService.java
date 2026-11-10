package br.com.agendaplay.service;
import br.com.agendaplay.exception.RegraNegocioException;
import br.com.agendaplay.model.*;
import br.com.agendaplay.security.UsuarioAutenticado;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
@Service
public class CaixaService {
    private final JdbcClient jdbc;
    private static final String SELECT="""
        SELECT c.id,c.id_estabelecimento,e.nome AS nome_estabelecimento,c.aberto_em,
            c.saldo_inicial,c.fechado_em,c.saldo_esperado,c.saldo_informado,c.observacao
        FROM caixa c JOIN estabelecimento e ON e.id=c.id_estabelecimento
        """;
    public CaixaService(JdbcClient jdbc) { this.jdbc=jdbc; }
    public static void validarValor(BigDecimal valor,boolean zeroPermitido) {
        if(valor==null || valor.scale()>2 || valor.signum()<0 || (!zeroPermitido && valor.signum()==0)
            || valor.compareTo(new BigDecimal("9999999999.99"))>0)
            throw new RegraNegocioException("Informe um valor válido em reais, com até duas casas decimais.");
    }
    public List<Caixa> listar(UsuarioAutenticado u) {
        validarPerfil(u);
        return jdbc.sql(SELECT+" WHERE e.id_proprietario=? ORDER BY c.aberto_em DESC,c.id DESC")
            .param(u.getId()).query(Caixa.class).list();
    }
    public List<Movimento> movimentos(long id,UsuarioAutenticado u) {
        propria(id,u);
        return jdbc.sql("""
            SELECT p.id_reserva,p.forma,p.valor,p.recebido_em AS data,'ENTRADA' AS tipo
            FROM recebimento p WHERE p.id_caixa=?
            UNION ALL SELECT p.id_reserva,p.forma,-p.valor,p.estornado_em,'ESTORNO'
            FROM recebimento p WHERE p.id_caixa_estorno=? ORDER BY data,id_reserva
            """).params(id,id).query(Movimento.class).list();
    }
    public Caixa propria(long id,UsuarioAutenticado u) {
        validarPerfil(u);
        return jdbc.sql(SELECT+" WHERE c.id=? AND e.id_proprietario=?").params(id,u.getId())
            .query(Caixa.class).optional()
            .orElseThrow(()->new AccessDeniedException("Este caixa não pertence à sua conta."));
    }
    private void validarPerfil(UsuarioAutenticado u) {
        if(u==null || u.getPerfil()!=Perfil.PROPRIETARIO)
            throw new AccessDeniedException("Acesso exclusivo do proprietário.");
    }
    private void bloquearEstabelecimento(long id,UsuarioAutenticado u) {
        validarPerfil(u);
        var dono=jdbc.sql("SELECT id_proprietario FROM estabelecimento WHERE id=? FOR UPDATE")
            .param(id).query(Long.class).optional();
        if(dono.isEmpty() || dono.get()!=u.getId())
            throw new AccessDeniedException("Este estabelecimento não pertence à sua conta.");
    }
    // Todas as operações bloqueiam estabelecimento, caixa e reserva nessa ordem.
    public long bloquearAberto(long estabelecimento,UsuarioAutenticado u) {
        bloquearEstabelecimento(estabelecimento,u);
        return jdbc.sql("SELECT id FROM caixa WHERE id_estabelecimento=? AND fechado_em IS NULL FOR UPDATE")
            .param(estabelecimento).query(Long.class).optional()
            .orElseThrow(()->new RegraNegocioException("Abra o caixa deste estabelecimento antes de registrar a operação."));
    }
    @Transactional
    public long abrir(long estabelecimento,BigDecimal inicial,UsuarioAutenticado u) {
        validarValor(inicial,true);
        bloquearEstabelecimento(estabelecimento,u);
        if(jdbc.sql("SELECT count(*) FROM caixa WHERE id_estabelecimento=? AND fechado_em IS NULL")
            .param(estabelecimento).query(Long.class).single()>0)
            throw new RegraNegocioException("Este estabelecimento já possui um caixa aberto.");
        return jdbc.sql("INSERT INTO caixa(id_estabelecimento,aberto_por,saldo_inicial) VALUES (?,?,?) RETURNING id")
            .params(estabelecimento,u.getId(),inicial).query(Long.class).single();
    }
    public Resumo resumo(long id,UsuarioAutenticado u) {
        var caixa=propria(id,u);
        var formas=jdbc.sql("""
            SELECT forma,sum(entrada) AS entradas,sum(saida) AS estornos FROM (
                SELECT forma,valor AS entrada,0 AS saida FROM recebimento WHERE id_caixa=?
                UNION ALL SELECT forma,0,valor FROM recebimento WHERE id_caixa_estorno=?
            ) m GROUP BY forma ORDER BY forma
            """).params(id,id).query(TotalForma.class).list();
        BigDecimal entradas=BigDecimal.ZERO,estornos=BigDecimal.ZERO,dinheiro=caixa.saldoInicial();
        for(var f:formas) {
            entradas=entradas.add(f.entradas()); estornos=estornos.add(f.estornos());
            if(f.forma().equals("DINHEIRO")) dinheiro=dinheiro.add(f.entradas()).subtract(f.estornos());
        }
        return new Resumo(caixa,formas,entradas,estornos,caixa.fechadoEm()==null?dinheiro:caixa.saldoEsperado());
    }
    @Transactional
    public void fechar(long id,BigDecimal informado,String observacao,UsuarioAutenticado u) {
        validarValor(informado,true);
        if(observacao!=null && observacao.length()>500)
            throw new RegraNegocioException("Use até 500 caracteres na observação.");
        var caixa=propria(id,u);
        long aberto=bloquearAberto(caixa.idEstabelecimento(),u);
        if(aberto!=id) throw new RegraNegocioException("Este caixa já está fechado.");
        var resumo=resumo(id,u);
        jdbc.sql("""
            UPDATE caixa SET fechado_em=current_timestamp,saldo_esperado=?,saldo_informado=?,observacao=?
            WHERE id=? AND fechado_em IS NULL
            """).params(resumo.dinheiroEsperado(),informado,observacao==null?"":observacao.trim(),id).update();
    }
    public record TotalForma(String forma,BigDecimal entradas,BigDecimal estornos) {}
    public record Resumo(Caixa caixa,List<TotalForma> formas,BigDecimal entradas,BigDecimal estornos,BigDecimal dinheiroEsperado) {}
    public record Movimento(long idReserva,String forma,BigDecimal valor,java.time.OffsetDateTime data,String tipo) {
        public Movimento { data=data.atZoneSameInstant(java.time.ZoneId.of("America/Sao_Paulo")).toOffsetDateTime(); }
    }
}
