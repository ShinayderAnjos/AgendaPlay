package br.com.agendaplay.service;

import br.com.agendaplay.exception.RegraNegocioException;
import br.com.agendaplay.model.Perfil;
import br.com.agendaplay.security.UsuarioAutenticado;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PainelFinanceiroService {
    private final JdbcClient jdbc;
    private final EstabelecimentoService estabelecimentos;
    public PainelFinanceiroService(JdbcClient jdbc,EstabelecimentoService estabelecimentos) {
        this.jdbc=jdbc; this.estabelecimentos=estabelecimentos;
    }
    private void validar(UsuarioAutenticado u,Long estabelecimento) {
        if(u==null || u.getPerfil()!=Perfil.PROPRIETARIO) throw new AccessDeniedException("Acesso exclusivo do proprietário.");
        if(estabelecimento!=null) estabelecimentos.propria(estabelecimento,u);
    }
    @Transactional
    public long despesa(long estabelecimento,String descricao,BigDecimal valor,LocalDate data,UsuarioAutenticado u) {
        validar(u,estabelecimento);
        CaixaService.validarValor(valor,false);
        if(descricao==null || descricao.isBlank() || descricao.trim().length()>200 || data==null)
            throw new RegraNegocioException("Informe descrição e data válidas para a despesa.");
        return jdbc.sql("INSERT INTO despesa(id_estabelecimento,descricao,valor,data) VALUES (?,?,?,?) RETURNING id")
                .params(estabelecimento,descricao.trim(),valor,data).query(Long.class).single();
    }
    public Resumo consultar(LocalDate inicio,LocalDate fim,Long estabelecimento,UsuarioAutenticado u) {
        validar(u,estabelecimento);
        if(inicio!=null && fim!=null && fim.isBefore(inicio)) throw new RegraNegocioException("Período inválido.");
        var receitas=jdbc.sql("""
            SELECT r.id AS id_reserva,r.data,e.nome AS estabelecimento,
                CASE WHEN r.situacao='CANCELADA' THEN 0 ELSE r.valor_total END AS valor_total,
                COALESCE((SELECT sum(p.valor) FROM recebimento p WHERE p.id_reserva=r.id AND p.estornado_em IS NULL),0) AS recebido
            FROM reserva r JOIN quadra q ON q.id=r.id_quadra JOIN estabelecimento e ON e.id=q.id_estabelecimento
            WHERE e.id_proprietario=:dono AND (CAST(:est AS BIGINT) IS NULL OR e.id=:est)
              AND (CAST(:inicio AS DATE) IS NULL OR r.data>=:inicio) AND (CAST(:fim AS DATE) IS NULL OR r.data<=:fim)
            ORDER BY r.data DESC,r.id DESC
            """).param("dono",u.getId()).param("est",estabelecimento).param("inicio",inicio).param("fim",fim)
            .query(Receita.class).list();
        var despesas=jdbc.sql("""
            SELECT d.id,d.data,e.nome AS estabelecimento,d.descricao,d.valor FROM despesa d
            JOIN estabelecimento e ON e.id=d.id_estabelecimento WHERE e.id_proprietario=:dono
            AND (CAST(:est AS BIGINT) IS NULL OR e.id=:est) AND (CAST(:inicio AS DATE) IS NULL OR d.data>=:inicio)
            AND (CAST(:fim AS DATE) IS NULL OR d.data<=:fim) ORDER BY d.data DESC,d.id DESC
            """).param("dono",u.getId()).param("est",estabelecimento).param("inicio",inicio).param("fim",fim)
            .query(Despesa.class).list();
        BigDecimal total=BigDecimal.ZERO,recebido=BigDecimal.ZERO,gasto=BigDecimal.ZERO;
        for(var r:receitas) { total=total.add(r.valorTotal()); recebido=recebido.add(r.recebido()); }
        for(var d:despesas) gasto=gasto.add(d.valor());
        return new Resumo(receitas,despesas,total,recebido,gasto,recebido.subtract(gasto));
    }
    public record Receita(long idReserva,LocalDate data,String estabelecimento,BigDecimal valorTotal,BigDecimal recebido) {}
    public record Despesa(long id,LocalDate data,String estabelecimento,String descricao,BigDecimal valor) {}
    public record Resumo(List<Receita> receitas,List<Despesa> despesas,BigDecimal receita,
                         BigDecimal recebido,BigDecimal despesasTotal,BigDecimal resultado) {}
}
