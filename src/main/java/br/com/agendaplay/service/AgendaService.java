package br.com.agendaplay.service;

import br.com.agendaplay.dto.PeriodoForm;
import br.com.agendaplay.exception.RegraNegocioException;
import br.com.agendaplay.model.*;
import br.com.agendaplay.repository.*;
import br.com.agendaplay.security.UsuarioAutenticado;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.*;
import java.time.*;
import java.util.*;

@Service
public class AgendaService {
    private final AgendaRepository agenda;
    private final QuadraRepository quadras;
    private final QuadraService quadraService;
    private final Clock relogio;
    private final PadraoService padroes;
    private final NotificacaoService notificacoes;
    private final long antecedencia;

    public AgendaService(
            AgendaRepository agenda,
            QuadraRepository quadras,
            QuadraService quadraService,
            Clock relogio,
            PadraoService padroes,
            NotificacaoService notificacoes,
            @Value("${agendaplay.cancelamento-antecedencia-minutos:0}") long antecedencia) {
        this.agenda = agenda;
        this.quadras = quadras;
        this.quadraService = quadraService;
        this.relogio = relogio;
        this.padroes = padroes;
        this.notificacoes = notificacoes;
        this.antecedencia = antecedencia;
    }

    private Quadra bloquear(long id) {
        return quadras.bloquear(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void validarPeriodo(PeriodoForm form) {
        if (form.getData() == null || form.getHoraInicio() == null || form.getHoraFim() == null)
            throw new RegraNegocioException("Preencha data e horários.");
        if (!form.getHoraFim().isAfter(form.getHoraInicio()))
            throw new RegraNegocioException(
                    "O horário final deve ser posterior ao inicial, no mesmo dia.");
        if (!LocalDateTime.of(form.getData(), form.getHoraInicio())
                .isAfter(LocalDateTime.now(relogio)))
            throw new RegraNegocioException("Escolha uma data e um horário no futuro.");
        if (form.getHoraInicio().getSecond() != 0
                || form.getHoraFim().getSecond() != 0
                || form.getHoraInicio().getNano() != 0
                || form.getHoraFim().getNano() != 0)
            throw new RegraNegocioException(
                    "Informe os horários em horas e minutos, sem segundos.");
    }

    private boolean sobrepoe(
            LocalTime inicio, LocalTime fim, LocalTime outroInicio, LocalTime outroFim) {
        return inicio.isBefore(outroFim) && fim.isAfter(outroInicio);
    }

    @Transactional
    public long disponibilizar(long idQuadra, PeriodoForm form, UsuarioAutenticado usuario) {
        var quadra = bloquear(idQuadra);
        quadraService.validarDono(quadra, usuario);
        validarPeriodo(form);
        if (!quadra.situacao().equals("ATIVA"))
            throw new RegraNegocioException("Ative a quadra antes de cadastrar horários.");
        boolean conflito =
                agenda.disponibilidades(idQuadra).stream()
                        .anyMatch(
                                d ->
                                        d.ativo()
                                                && d.data().equals(form.getData())
                                                && sobrepoe(
                                                        form.getHoraInicio(),
                                                        form.getHoraFim(),
                                                        d.horaInicio(),
                                                        d.horaFim()));
        if (conflito)
            throw new RegraNegocioException("Já existe uma disponibilidade nesse intervalo.");
        return agenda.cadastrarDisponibilidade(
                idQuadra,
                form.getData(),
                form.getHoraInicio(),
                form.getHoraFim(),
                form.getValorHora() == null ? quadra.valorHora() : form.getValorHora(),
                form.getToleranciaMinutos() == null
                        ? quadra.toleranciaMinutos()
                        : form.getToleranciaMinutos());
    }

    public List<Disponibilidade> disponibilidades(long quadra, UsuarioAutenticado usuario) {
        quadraService.propria(quadra, usuario);
        padroes.renovar(quadra);
        return agenda.disponibilidades(quadra);
    }

    @Transactional
    public void inativarDisponibilidade(long quadraId, long id, UsuarioAutenticado usuario) {
        quadraService.validarDono(bloquear(quadraId), usuario);
        var janela =
                agenda.disponibilidades(quadraId).stream()
                        .filter(d -> d.id() == id)
                        .findFirst()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean possuiReserva =
                agenda.reservasDaQuadra(quadraId).stream()
                        .anyMatch(
                                r ->
                                        r.situacao().equals("CONFIRMADA")
                                                && r.data().equals(janela.data())
                                                && sobrepoe(
                                                        r.horaInicio(),
                                                        r.horaFim(),
                                                        janela.horaInicio(),
                                                        janela.horaFim()));
        if (possuiReserva)
            throw new RegraNegocioException(
                    "Esta disponibilidade possui reservas confirmadas. Cancele-as antes de"
                            + " inativar.");
        agenda.inativarDisponibilidade(id, quadraId);
    }

    public List<HorarioLivre> horariosLivres(long quadraId) {
        var q = quadraService.buscar(quadraId);
        if (!q.situacao().equals("ATIVA")) return List.of();
        padroes.renovar(quadraId);
        var reservas =
                agenda.reservasDaQuadra(quadraId).stream()
                        .filter(r -> r.situacao().equals("CONFIRMADA"))
                        .toList();
        var agora = LocalDateTime.now(relogio);
        List<HorarioLivre> livres = new ArrayList<>();
        for (var d : agenda.disponibilidades(quadraId)) {
            if (!d.ativo() || !d.data().atTime(d.horaFim()).isAfter(agora)) continue;
            var inicio = d.data().atTime(d.horaInicio());
            var fim = d.data().atTime(d.horaFim());
            if (!inicio.isAfter(agora)) inicio = agora.plusMinutes(1).withSecond(0).withNano(0);
            if (!inicio.isBefore(fim)) continue;
            List<LocalDateTime[]> partes = new ArrayList<>();
            partes.add(new LocalDateTime[] {inicio, fim});
            for (var r : reservas) {
                var ri = r.data().atTime(r.horaInicio()).minusMinutes(d.toleranciaMinutos());
                var rf = r.data().atTime(r.horaFim()).plusMinutes(r.toleranciaMinutos());
                List<LocalDateTime[]> restantes = new ArrayList<>();
                for (var a : partes) {
                    if (!a[0].isBefore(rf) || !a[1].isAfter(ri)) {
                        restantes.add(a);
                        continue;
                    }
                    if (a[0].isBefore(ri)) restantes.add(new LocalDateTime[] {a[0], ri});
                    if (a[1].isAfter(rf)) restantes.add(new LocalDateTime[] {rf, a[1]});
                }
                partes = restantes;
            }
            for (var a : partes)
                livres.add(
                        new HorarioLivre(
                                d.data(),
                                a[0].toLocalTime(),
                                a[1].toLocalTime(),
                                d.valorHora() == null ? q.valorHora() : d.valorHora(),
                                d.toleranciaMinutos()));
        }
        return livres;
    }

    @Transactional
    public long reservar(long quadraId, PeriodoForm form, UsuarioAutenticado usuario) {
        if (usuario.getPerfil() != Perfil.CLIENTE)
            throw new AccessDeniedException("Apenas clientes podem reservar.");
        var quadra = bloquear(quadraId);
        validarPeriodo(form);
        if (!quadra.situacao().equals("ATIVA"))
            throw new RegraNegocioException("Esta quadra está inativa.");
        // Reconsulta sob bloqueio: a tela pode estar desatualizada quando o cliente confirma.
        var janela =
                horariosLivres(quadraId).stream()
                        .filter(
                                h ->
                                        h.data().equals(form.getData())
                                                && !form.getHoraInicio().isBefore(h.horaInicio())
                                                && !form.getHoraFim().isAfter(h.horaFim()))
                        .findFirst();
        if (janela.isEmpty())
            throw new RegraNegocioException(
                    "Este intervalo não está mais disponível. Escolha outro horário.");
        long minutos = Duration.between(form.getHoraInicio(), form.getHoraFim()).toMinutes();
        var valor =
                janela.orElseThrow()
                        .valorHora()
                        .multiply(BigDecimal.valueOf(minutos))
                        .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        return agenda.reservar(
                usuario.getId(),
                quadraId,
                form.getData(),
                form.getHoraInicio(),
                form.getHoraFim(),
                valor,
                janela.orElseThrow().toleranciaMinutos());
    }

    public List<Reserva> minhasReservas(UsuarioAutenticado usuario, Long quadra, LocalDate data) {
        return usuario.getPerfil() == Perfil.CLIENTE
                ? agenda.reservasDoCliente(usuario.getId())
                : agenda.reservasDoProprietario(usuario.getId(), quadra, data);
    }

    @Transactional
    public void cancelar(long id, UsuarioAutenticado usuario) {
        var original =
                agenda.buscarReserva(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var quadra = bloquear(original.idQuadra());
        var reserva = agenda.buscarReserva(id).orElseThrow();
        boolean cliente =
                usuario.getPerfil() == Perfil.CLIENTE && reserva.idCliente() == usuario.getId();
        boolean dono =
                usuario.getPerfil() == Perfil.PROPRIETARIO
                        && quadra.idProprietario() == usuario.getId();
        if (!cliente && !dono)
            throw new AccessDeniedException("Esta reserva não pertence à sua conta.");
        if (reserva.situacao().equals("CANCELADA")) return;
        if (!LocalDateTime.of(reserva.data(), reserva.horaInicio())
                .isAfter(LocalDateTime.now(relogio).plusMinutes(antecedencia)))
            throw new RegraNegocioException("O prazo de cancelamento desta reserva terminou.");
        agenda.cancelar(id);
        notificacoes.cancelamento(reserva, quadra, usuario);
    }
}
