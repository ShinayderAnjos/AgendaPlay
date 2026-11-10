package br.com.agendaplay.repository;
import br.com.agendaplay.model.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
@Repository
public class FinanceiroRepository {
    private final JdbcClient jdbc;
    public FinanceiroRepository(JdbcClient jdbc) { this.jdbc=jdbc; }
    public List<LancamentoFinanceiro> listar(long usuario,boolean cliente,LocalDate inicio,LocalDate fim) {
        return jdbc.sql("""
            SELECT r.id AS id_reserva,r.data,r.hora_inicio,q.nome AS nome_quadra,u.nome AS nome_cliente,
                e.nome AS nome_estabelecimento,e.id AS id_estabelecimento,r.situacao,r.valor_total,
                COALESCE(p.recebido,0) AS valor_recebido,COALESCE(p.estornado,0) AS valor_estornado,p.forma
            FROM reserva r JOIN quadra q ON q.id=r.id_quadra
            JOIN estabelecimento e ON e.id=q.id_estabelecimento JOIN usuario u ON u.id=r.id_cliente
            LEFT JOIN LATERAL (
                SELECT sum(valor) AS recebido,
                    sum(CASE WHEN estornado_em IS NOT NULL THEN valor ELSE 0 END) AS estornado,
                    string_agg(DISTINCT forma,', ' ORDER BY forma) AS forma
                FROM recebimento WHERE id_reserva=r.id
            ) p ON true WHERE
            """+(cliente?" r.id_cliente=:usuario ":" q.id_proprietario=:usuario ")+"""
            AND (CAST(:inicio AS DATE) IS NULL OR r.data>=:inicio)
            AND (CAST(:fim AS DATE) IS NULL OR r.data<:fim)
            ORDER BY r.data DESC,r.hora_inicio DESC,r.id DESC
            """).param("usuario",usuario).param("inicio",inicio).param("fim",fim)
            .query(LancamentoFinanceiro.class).list();
    }
    public List<ParcelaFinanceira> parcelas(long usuario,boolean cliente) {
        return jdbc.sql("""
            SELECT p.id,p.id_reserva,p.valor,p.forma,p.recebido_em,p.estornado_em,p.id_caixa,p.id_caixa_estorno
            FROM recebimento p JOIN reserva r ON r.id=p.id_reserva
            JOIN quadra q ON q.id=r.id_quadra WHERE
            """+(cliente?"r.id_cliente=?":"q.id_proprietario=?")+" ORDER BY p.recebido_em,p.id")
            .param(usuario).query(ParcelaFinanceira.class).list();
    }
    public Optional<EstadoReserva> estado(long id,boolean bloquear) {
        return jdbc.sql("""
            SELECT q.id_proprietario,q.id_estabelecimento,r.situacao,r.valor_total,
                COALESCE((SELECT sum(valor) FROM recebimento WHERE id_reserva=r.id),0) AS recebido,
                COALESCE((SELECT sum(valor) FROM recebimento WHERE id_reserva=r.id
                    AND estornado_em IS NOT NULL),0) AS estornado
            FROM reserva r JOIN quadra q ON q.id=r.id_quadra WHERE r.id=?
            """+(bloquear?" FOR UPDATE OF r":"")).param(id).query(EstadoReserva.class).optional();
    }
    public Optional<ParcelaFinanceira> pelaChave(UUID chave) {
        return jdbc.sql("""
            SELECT id,id_reserva,valor,forma,recebido_em,estornado_em,id_caixa,id_caixa_estorno
            FROM recebimento WHERE chave=?
            """).param(chave).query(ParcelaFinanceira.class).optional();
    }
    public void registrar(long id,String forma,BigDecimal valor,long caixa,UUID chave) {
        jdbc.sql("INSERT INTO recebimento(id_reserva,forma,valor,id_caixa,chave) VALUES (?,?,?,?,?)")
            .params(id,forma,valor,caixa,chave).update();
    }
    public void estornar(long id,long caixa) {
        jdbc.sql("""
            UPDATE recebimento SET estornado_em=current_timestamp,id_caixa_estorno=?
            WHERE id_reserva=? AND estornado_em IS NULL
            """).params(caixa,id).update();
    }
    public record EstadoReserva(long idProprietario,long idEstabelecimento,String situacao,
        BigDecimal valorTotal,BigDecimal recebido,BigDecimal estornado) {}
}
