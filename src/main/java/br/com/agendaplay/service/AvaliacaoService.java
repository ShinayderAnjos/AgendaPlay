package br.com.agendaplay.service;

import br.com.agendaplay.exception.RegraNegocioException;
import br.com.agendaplay.model.*;
import br.com.agendaplay.repository.*;
import br.com.agendaplay.security.UsuarioAutenticado;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;

@Service
public class AvaliacaoService {
    private final JdbcClient jdbc;
    private final AgendaRepository agenda;
    private final QuadraService quadras;
    private final Clock clock;

    public AvaliacaoService(
            JdbcClient jdbc, AgendaRepository agenda, QuadraService quadras, Clock clock) {
        this.jdbc = jdbc;
        this.agenda = agenda;
        this.quadras = quadras;
        this.clock = clock;
    }

    private Reserva bloquear(long id) {
        var original =
                agenda.buscarReserva(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        jdbc.sql("SELECT id FROM quadra WHERE id=? FOR UPDATE")
                .param(original.idQuadra())
                .query(Long.class)
                .single();
        jdbc.sql("SELECT id FROM reserva WHERE id=? FOR UPDATE")
                .param(id)
                .query(Long.class)
                .optional()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return agenda.buscarReserva(id).orElseThrow();
    }

    @Transactional
    public void concluir(long id, UsuarioAutenticado usuario) {
        var r = bloquear(id);
        quadras.propria(r.idQuadra(), usuario);
        if (r.situacao().equals("CONCLUIDA")) return;
        if (!r.situacao().equals("CONFIRMADA"))
            throw new RegraNegocioException("Somente reservas confirmadas podem ser concluídas.");
        if (r.data().atTime(r.horaInicio()).isAfter(LocalDateTime.now(clock)))
            throw new RegraNegocioException(
                    "A baixa fica disponível a partir do início da reserva, após confirmar o comparecimento do cliente.");
        jdbc.sql("UPDATE reserva SET situacao='CONCLUIDA', concluida_em=? WHERE id=?")
                .params(OffsetDateTime.now(clock), id)
                .update();
    }

    @Transactional
    public void avaliar(long id, int nota, String comentario, UsuarioAutenticado usuario) {
        var r = bloquear(id);
        if (usuario.getPerfil() != Perfil.CLIENTE || r.idCliente() != usuario.getId())
            throw new AccessDeniedException("Esta reserva não pertence à sua conta.");
        if (!r.situacao().equals("CONCLUIDA"))
            throw new RegraNegocioException(
                    "A avaliação é liberada após o proprietário concluir a reserva.");
        if (nota < 1 || nota > 5) throw new RegraNegocioException("Escolha uma nota de 1 a 5.");
        if (comentario == null) comentario = "";
        if (comentario.length() > 1000)
            throw new RegraNegocioException("Use até 1.000 caracteres no comentário.");
        int n =
                jdbc.sql(
                                "INSERT INTO avaliacao(id_reserva,nota,comentario) VALUES (?,?,?)"
                                    + " ON CONFLICT (id_reserva) DO NOTHING")
                        .params(id, nota, comentario.trim())
                        .update();
        if (n == 0) throw new RegraNegocioException("Esta reserva já foi avaliada.");
    }

    public record Avaliacao(long idReserva, int nota, String comentario) {}

    public List<Avaliacao> daQuadra(long id) {
        return jdbc.sql(
                        "SELECT a.id_reserva,a.nota,a.comentario FROM avaliacao a JOIN reserva r ON"
                            + " r.id=a.id_reserva WHERE r.id_quadra=? ORDER BY a.criado_em DESC")
                .param(id)
                .query(Avaliacao.class)
                .list();
    }

    public Map<Long, Avaliacao> doCliente(long id) {
        var m = new HashMap<Long, Avaliacao>();
        jdbc.sql(
                        "SELECT a.id_reserva,a.nota,a.comentario FROM avaliacao a JOIN reserva r ON"
                            + " r.id=a.id_reserva WHERE r.id_cliente=?")
                .param(id)
                .query(Avaliacao.class)
                .list()
                .forEach(a -> m.put(a.idReserva(), a));
        return m;
    }
}
