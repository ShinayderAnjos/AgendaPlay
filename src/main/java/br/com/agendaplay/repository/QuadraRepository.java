package br.com.agendaplay.repository;

import br.com.agendaplay.model.Quadra;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class QuadraRepository {
    private final JdbcClient jdbc;

    public QuadraRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Quadra> listarAtivas() {
        return jdbc.sql("SELECT * FROM quadra WHERE situacao='ATIVA' ORDER BY nome,id")
                .query(Quadra.class)
                .list();
    }

    public List<Quadra> listarDoProprietario(long id) {
        return jdbc.sql("SELECT * FROM quadra WHERE id_proprietario=? ORDER BY nome,id")
                .param(id)
                .query(Quadra.class)
                .list();
    }

    public Optional<Quadra> buscar(long id) {
        return jdbc.sql("SELECT * FROM quadra WHERE id=?").param(id).query(Quadra.class).optional();
    }

    public Optional<Quadra> bloquear(long id) {
        return jdbc.sql("SELECT * FROM quadra WHERE id=? FOR UPDATE")
                .param(id)
                .query(Quadra.class)
                .optional();
    }

    public long salvar(Quadra q) {
        String sql =
                q.id() == 0
                        ? "INSERT INTO"
                              + " quadra(id_proprietario,nome,modalidade,localizacao,valor_hora,situacao,id_estabelecimento,tolerancia_minutos,latitude,longitude)"
                              + " VALUES(:u,:n,:m,:l,:v,:s,:e,:t,:a,:o) RETURNING id"
                        : "UPDATE quadra SET"
                              + " nome=:n,modalidade=:m,localizacao=:l,valor_hora=:v,situacao=:s,id_estabelecimento=:e,tolerancia_minutos=:t,latitude=:a,longitude=:o"
                              + " WHERE id=:id AND id_proprietario=:u RETURNING id";
        var c =
                jdbc.sql(sql)
                        .param("u", q.idProprietario())
                        .param("n", q.nome())
                        .param("m", q.modalidade())
                        .param("l", q.localizacao())
                        .param("v", q.valorHora())
                        .param("s", q.situacao())
                        .param("e", q.idEstabelecimento())
                        .param("t", q.toleranciaMinutos())
                        .param("a", q.latitude())
                        .param("o", q.longitude());
        if (q.id() != 0) c.param("id", q.id());
        return c.query(Long.class).single();
    }
}
