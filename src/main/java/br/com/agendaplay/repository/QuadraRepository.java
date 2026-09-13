package br.com.agendaplay.repository;

import br.com.agendaplay.model.Quadra;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class QuadraRepository {
    private final JdbcClient jdbc;

    public QuadraRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Quadra> listarAtivas() {
        return jdbc.sql("SELECT * FROM quadra WHERE situacao = 'ATIVA' ORDER BY nome, id")
                .query(Quadra.class)
                .list();
    }

    public List<Quadra> listarDoProprietario(long id) {
        return jdbc.sql("SELECT * FROM quadra WHERE id_proprietario = ? ORDER BY nome, id")
                .param(id)
                .query(Quadra.class)
                .list();
    }

    public Optional<Quadra> buscar(long id) {
        return jdbc.sql("SELECT * FROM quadra WHERE id = ?")
                .param(id)
                .query(Quadra.class)
                .optional();
    }

    public Optional<Quadra> bloquear(long id) {
        // Todas as mudanças de agenda passam pelo mesmo bloqueio da quadra.
        return jdbc.sql("SELECT * FROM quadra WHERE id = ? FOR UPDATE")
                .param(id)
                .query(Quadra.class)
                .optional();
    }

    public long salvar(Quadra q) {
        if (q.id() == 0) {
            return jdbc.sql(
                            """
                            INSERT INTO quadra(id_proprietario, nome, modalidade, localizacao, valor_hora, situacao)
                            VALUES (?, ?, ?, ?, ?, ?) RETURNING id
                            """)
                    .params(
                            q.idProprietario(),
                            q.nome(),
                            q.modalidade(),
                            q.localizacao(),
                            q.valorHora(),
                            q.situacao())
                    .query(Long.class)
                    .single();
        }
        jdbc.sql(
                        "UPDATE quadra SET nome=?, modalidade=?, localizacao=?, valor_hora=?,"
                            + " situacao=? WHERE id=? AND id_proprietario=?")
                .params(
                        q.nome(),
                        q.modalidade(),
                        q.localizacao(),
                        q.valorHora(),
                        q.situacao(),
                        q.id(),
                        q.idProprietario())
                .update();
        return q.id();
    }
}
