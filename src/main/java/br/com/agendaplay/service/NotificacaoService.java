package br.com.agendaplay.service;

import br.com.agendaplay.model.*;
import br.com.agendaplay.security.UsuarioAutenticado;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class NotificacaoService {
    private final JdbcClient jdbc;

    public NotificacaoService(JdbcClient j) {
        jdbc = j;
    }

    public List<Notificacao> pendentes(long u) {
        return jdbc.sql(
                        "SELECT * FROM notificacao WHERE id_usuario=? AND NOT lida ORDER BY"
                            + " criada_em DESC,id DESC")
                .param(u)
                .query(Notificacao.class)
                .list();
    }

    public void ler(long id, long u) {
        jdbc.sql("UPDATE notificacao SET lida=true WHERE id=? AND id_usuario=?")
                .params(id, u)
                .update();
    }

    public void cancelamento(Reserva r, Quadra q, UsuarioAutenticado autor) {
        long destino = autor.getPerfil() == Perfil.CLIENTE ? q.idProprietario() : r.idCliente();
        String mensagem =
                "Reserva #"
                        + r.id()
                        + " de "
                        + q.nome()
                        + " em "
                        + r.data()
                                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        + ", "
                        + r.horaInicio()
                        + "–"
                        + r.horaFim()
                        + ", cancelada pelo "
                        + (autor.getPerfil() == Perfil.CLIENTE ? "cliente." : "proprietário.");
        jdbc.sql(
                        "INSERT INTO notificacao(id_usuario,id_reserva,mensagem) VALUES(?,?,?) ON"
                            + " CONFLICT(id_usuario,id_reserva) DO NOTHING")
                .params(destino, r.id(), mensagem)
                .update();
    }
}
