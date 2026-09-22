package br.com.agendaplay.repository;

import br.com.agendaplay.model.*;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Repository
public class AgendaRepository {
    private final JdbcClient jdbc;
    private static final String RESERVAS =
            """
            SELECT r.*, q.nome AS nome_quadra, u.nome AS nome_cliente
            FROM reserva r JOIN quadra q ON q.id=r.id_quadra JOIN usuario u ON u.id=r.id_cliente
            """;

    public AgendaRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Disponibilidade> disponibilidades(long quadra) {
        return jdbc.sql(
                        "SELECT * FROM disponibilidade WHERE id_quadra=? ORDER BY data,"
                                + " hora_inicio")
                .param(quadra)
                .query(Disponibilidade.class)
                .list();
    }

    public long cadastrarDisponibilidade(
            long quadra, LocalDate data, LocalTime inicio, LocalTime fim) {
        return jdbc.sql(
                        "INSERT INTO disponibilidade(id_quadra,data,hora_inicio,hora_fim) VALUES"
                                + " (?,?,?,?) RETURNING id")
                .params(quadra, data, inicio, fim)
                .query(Long.class)
                .single();
    }

    public long cadastrarDisponibilidade(
            long quadra,
            LocalDate data,
            LocalTime inicio,
            LocalTime fim,
            BigDecimal valor,
            int tolerancia) {
        return jdbc.sql(
                        "INSERT INTO"
                            + " disponibilidade(id_quadra,data,hora_inicio,hora_fim,valor_hora,tolerancia_minutos)"
                            + " VALUES(:q,:d,:i,:f,:v,:t) RETURNING id")
                .param("q", quadra)
                .param("d", data)
                .param("i", inicio)
                .param("f", fim)
                .param("v", valor)
                .param("t", tolerancia)
                .query(Long.class)
                .single();
    }

    public long reservar(
            long cliente,
            long quadra,
            LocalDate data,
            LocalTime inicio,
            LocalTime fim,
            BigDecimal valor,
            int tolerancia) {
        return jdbc.sql(
                        "INSERT INTO"
                            + " reserva(id_cliente,id_quadra,data,hora_inicio,hora_fim,valor_total,tolerancia_minutos)"
                            + " VALUES(?,?,?,?,?,?,?) RETURNING id")
                .params(cliente, quadra, data, inicio, fim, valor, tolerancia)
                .query(Long.class)
                .single();
    }

    public void inativarDisponibilidade(long id, long quadra) {
        jdbc.sql("UPDATE disponibilidade SET ativo=false WHERE id=? AND id_quadra=?")
                .params(id, quadra)
                .update();
    }

    public List<Reserva> reservasDaQuadra(long quadra) {
        return jdbc.sql(RESERVAS + " WHERE r.id_quadra=? ORDER BY r.data,r.hora_inicio")
                .param(quadra)
                .query(Reserva.class)
                .list();
    }

    public List<Reserva> reservasDoCliente(long cliente) {
        return jdbc.sql(RESERVAS + " WHERE r.id_cliente=? ORDER BY r.data DESC,r.hora_inicio")
                .param(cliente)
                .query(Reserva.class)
                .list();
    }

    public List<Reserva> reservasDoProprietario(long proprietario, Long quadra, LocalDate data) {
        var sql = RESERVAS + " WHERE q.id_proprietario=:proprietario";
        if (quadra != null) sql += " AND q.id=:quadra";
        if (data != null) sql += " AND r.data=:data";
        var consulta =
                jdbc.sql(sql + " ORDER BY r.data DESC,r.hora_inicio")
                        .param("proprietario", proprietario);
        if (quadra != null) consulta.param("quadra", quadra);
        if (data != null) consulta.param("data", data);
        return consulta.query(Reserva.class).list();
    }

    public Optional<Reserva> buscarReserva(long id) {
        return jdbc.sql(RESERVAS + " WHERE r.id=?").param(id).query(Reserva.class).optional();
    }

    public long reservar(
            long cliente,
            long quadra,
            LocalDate data,
            LocalTime inicio,
            LocalTime fim,
            BigDecimal valor) {
        return jdbc.sql(
                        """
                        INSERT INTO reserva(id_cliente,id_quadra,data,hora_inicio,hora_fim,valor_total)
                        VALUES (?,?,?,?,?,?) RETURNING id
                        """)
                .params(cliente, quadra, data, inicio, fim, valor)
                .query(Long.class)
                .single();
    }

    public void cancelar(long id) {
        jdbc.sql("UPDATE reserva SET situacao='CANCELADA' WHERE id=?").param(id).update();
    }
}
