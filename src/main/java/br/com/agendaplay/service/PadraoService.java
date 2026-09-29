package br.com.agendaplay.service;

import br.com.agendaplay.dto.PadraoForm;
import br.com.agendaplay.exception.RegraNegocioException;
import br.com.agendaplay.model.*;
import br.com.agendaplay.repository.*;
import br.com.agendaplay.security.UsuarioAutenticado;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
public class PadraoService {
    private final JdbcClient jdbc;
    private final QuadraRepository quadras;
    private final QuadraService service;
    private final Clock clock;

    public PadraoService(JdbcClient j, QuadraRepository q, QuadraService s, Clock c) {
        jdbc = j;
        quadras = q;
        service = s;
        clock = c;
    }

    public List<Padrao> listar(long q) {
        return jdbc.sql("SELECT * FROM padrao_disponibilidade WHERE id_quadra=? ORDER BY id")
                .param(q)
                .query(Padrao.class)
                .list();
    }

    public List<Long> quadrasAtivas() {
        return jdbc.sql(
                        "SELECT DISTINCT p.id_quadra FROM padrao_disponibilidade p JOIN quadra q ON"
                            + " q.id=p.id_quadra WHERE p.ativo AND q.situacao='ATIVA' ORDER BY"
                            + " p.id_quadra")
                .query(Long.class)
                .list();
    }

    @Transactional
    public long criar(long q, PadraoForm f, UsuarioAutenticado u) {
        var quadra = quadras.bloquear(q).orElseThrow();
        service.validarDono(quadra, u);
        if (!quadra.situacao().equals("ATIVA"))
            throw new RegraNegocioException("Ative a quadra antes de criar padrões.");
        if (f.getDias() == null
                || f.getDias().isEmpty()
                || f.getDias().stream().anyMatch(d -> d == null || d < 1 || d > 7)
                || f.getHoraInicio() == null
                || f.getHoraFim() == null
                || !f.getHoraFim().isAfter(f.getHoraInicio())
                || f.getHoraInicio().getSecond() != 0
                || f.getHoraFim().getSecond() != 0
                || f.getDuracaoMinutos() < 1
                || f.getDuracaoMinutos() > 1440
                || f.getToleranciaMinutos() < 0
                || f.getToleranciaMinutos() > 1440
                || f.getValorHora() == null
                || f.getValorHora().signum() < 0
                || Duration.between(f.getHoraInicio(), f.getHoraFim()).toMinutes()
                        < f.getDuracaoMinutos())
            throw new RegraNegocioException(
                    "Confira dias, duração, valor, tolerância e horários do padrão.");
        for (var p : listar(q))
            if (p.ativo()
                    && f.getDias().stream()
                            .anyMatch(
                                    d -> Arrays.asList(p.dias().split(",")).contains(d.toString()))
                    && f.getHoraInicio().isBefore(p.horaFim())
                    && f.getHoraFim().isAfter(p.horaInicio()))
                throw new RegraNegocioException(
                        "Já existe um padrão ativo nesses dias e horários. Desative-o antes de"
                            + " substituir.");
        String dias =
                f.getDias().stream()
                        .distinct()
                        .sorted()
                        .map(Object::toString)
                        .collect(java.util.stream.Collectors.joining(","));
        long id =
                jdbc.sql(
                                "INSERT INTO"
                                    + " padrao_disponibilidade(id_quadra,dias,hora_inicio,hora_fim,duracao_minutos,tolerancia_minutos,valor_hora)"
                                    + " VALUES(?,?,?,?,?,?,?) RETURNING id")
                        .params(
                                q,
                                dias,
                                f.getHoraInicio(),
                                f.getHoraFim(),
                                f.getDuracaoMinutos(),
                                f.getToleranciaMinutos(),
                                f.getValorHora())
                        .query(Long.class)
                        .single();
        gerarBloqueado(q);
        return id;
    }

    @Transactional
    public void renovar(long q) {
        quadras.bloquear(q).orElseThrow();
        gerarBloqueado(q);
    }

    private void gerarBloqueado(long q) {
        if (!quadras.buscar(q).orElseThrow().situacao().equals("ATIVA")) return;
        LocalDate hoje = LocalDate.now(clock),
                ate = hoje.plusMonths(1).withDayOfMonth(1).plusMonths(1).minusDays(1);
        for (var p : listar(q)) {
            if (!p.ativo()) continue;
            var dias = Arrays.asList(p.dias().split(","));
            for (LocalDate d = hoje; !d.isAfter(ate); d = d.plusDays(1)) {
                if (!dias.contains(String.valueOf(d.getDayOfWeek().getValue()))) continue;
                int limite = p.horaFim().toSecondOfDay() / 60;
                for (int m = p.horaInicio().toSecondOfDay() / 60;
                        m + p.duracaoMinutos() <= limite;
                        m += p.duracaoMinutos() + p.toleranciaMinutos()) {
                    LocalTime i = LocalTime.of(m / 60, m % 60),
                            f = i.plusMinutes(p.duracaoMinutos());
                    if (!d.atTime(i).isAfter(LocalDateTime.now(clock))) continue;
                    jdbc.sql(
                                    """
                                    INSERT INTO disponibilidade(id_quadra,data,hora_inicio,hora_fim,id_padrao,valor_hora,tolerancia_minutos)
                                    SELECT :q,:d,:i,:f,:p,:v,:t WHERE NOT EXISTS(
                                     SELECT 1 FROM disponibilidade WHERE id_quadra=:q AND data=:d AND ativo AND hora_inicio<:f AND hora_fim>:i)
                                     AND NOT EXISTS(SELECT 1 FROM reserva WHERE id_quadra=:q AND data=:d AND situacao='CONFIRMADA' AND hora_inicio<:f AND hora_fim>:i)
                                    ON CONFLICT(id_padrao,data,hora_inicio) DO NOTHING
                                    """)
                            .param("q", q)
                            .param("d", d)
                            .param("i", i)
                            .param("f", f)
                            .param("p", p.id())
                            .param("v", p.valorHora())
                            .param("t", p.toleranciaMinutos())
                            .update();
                }
            }
        }
    }

    @Transactional
    public void desativar(long q, long id, UsuarioAutenticado u) {
        service.validarDono(quadras.bloquear(q).orElseThrow(), u);
        if (listar(q).stream().noneMatch(p -> p.id() == id))
            throw new RegraNegocioException("Padrão não encontrado nesta quadra.");
        jdbc.sql("UPDATE padrao_disponibilidade SET ativo=false WHERE id=? AND id_quadra=?")
                .params(id, q)
                .update();
        // Reservas existentes e suas janelas permanecem; horários futuros vazios são retirados.
        jdbc.sql(
                        """
                        UPDATE disponibilidade d SET ativo=false WHERE d.id_padrao=:p AND d.data+d.hora_inicio>:agora
                         AND NOT EXISTS(SELECT 1 FROM reserva r WHERE r.id_quadra=d.id_quadra AND r.data=d.data AND r.situacao='CONFIRMADA' AND r.hora_inicio<d.hora_fim AND r.hora_fim>d.hora_inicio)
                        """)
                .param("p", id)
                .param("agora", LocalDateTime.now(clock))
                .update();
    }
}
