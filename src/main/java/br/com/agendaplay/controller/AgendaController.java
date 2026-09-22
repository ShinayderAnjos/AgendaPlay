package br.com.agendaplay.controller;

import br.com.agendaplay.dto.PeriodoForm;
import br.com.agendaplay.exception.RegraNegocioException;
import br.com.agendaplay.model.Perfil;
import br.com.agendaplay.security.UsuarioAutenticado;
import br.com.agendaplay.service.*;

import jakarta.validation.Valid;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
public class AgendaController {
    private final AgendaService agenda;
    private final QuadraService quadras;
    private final EstabelecimentoService estabelecimentos;
    private final java.time.Clock clock;

    public AgendaController(
            AgendaService agenda,
            QuadraService quadras,
            EstabelecimentoService estabelecimentos,
            java.time.Clock clock) {
        this.estabelecimentos = estabelecimentos;
        this.clock = clock;
        this.agenda = agenda;
        this.quadras = quadras;
    }

    private void carregar(Model model, long id, UsuarioAutenticado usuario, boolean dono) {
        model.addAttribute("quadra", dono ? quadras.propria(id, usuario) : quadras.buscar(id));
        model.addAttribute("dono", dono);
        if (dono) model.addAttribute("disponibilidades", agenda.disponibilidades(id, usuario));
        else model.addAttribute("horarios", agenda.horariosLivres(id));
    }

    @GetMapping({"/proprietario/quadras/{id}/agenda", "/cliente/quadras/{id}/horarios"})
    String horarios(
            @PathVariable long id,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            Model model) {
        carregar(model, id, usuario, usuario.getPerfil() == Perfil.PROPRIETARIO);
        model.addAttribute("form", new PeriodoForm());
        return "horarios";
    }

    @PostMapping({"/proprietario/quadras/{id}/agenda", "/cliente/quadras/{id}/reservar"})
    String criar(
            @PathVariable long id,
            @Valid @ModelAttribute("form") PeriodoForm form,
            BindingResult erros,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            Model model,
            RedirectAttributes flash) {
        boolean dono = usuario.getPerfil() == Perfil.PROPRIETARIO;
        if (!erros.hasErrors()) {
            try {
                if (dono) agenda.disponibilizar(id, form, usuario);
                else agenda.reservar(id, form, usuario);
                flash.addFlashAttribute(
                        "sucesso",
                        dono
                                ? "Disponibilidade cadastrada."
                                : "Reserva confirmada! Confira os detalhes abaixo.");
                return dono
                        ? "redirect:/proprietario/quadras/" + id + "/agenda"
                        : "redirect:/reservas";
            } catch (RegraNegocioException e) {
                erros.reject("periodo", e.getMessage());
            } catch (DataIntegrityViolationException e) {
                erros.reject(
                        "conflito",
                        "Este horário acabou de ser ocupado ou já está cadastrado. Escolha outro"
                                + " intervalo.");
            }
        }
        carregar(model, id, usuario, dono);
        return "horarios";
    }

    @PostMapping("/proprietario/quadras/{quadra}/disponibilidades/{id}/inativar")
    String inativar(
            @PathVariable long quadra,
            @PathVariable long id,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            RedirectAttributes flash) {
        try {
            agenda.inativarDisponibilidade(quadra, id, usuario);
            flash.addFlashAttribute("sucesso", "Disponibilidade inativada.");
        } catch (RegraNegocioException e) {
            flash.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/proprietario/quadras/" + quadra + "/agenda";
    }

    @GetMapping("/reservas")
    String reservas(
            @RequestParam(required = false) Long quadra,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate data,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            Model model) {
        model.addAttribute("reservas", agenda.minhasReservas(usuario, quadra, data));
        model.addAttribute("dono", usuario.getPerfil() == Perfil.PROPRIETARIO);
        model.addAttribute("quadras", quadras.minhas(usuario.getId()));
        model.addAttribute("filtroQuadra", quadra);
        model.addAttribute("filtroData", data);
        return "reservas";
    }

    @GetMapping("/proprietario/agenda")
    String agenda(
            @RequestParam(required = false) Long estabelecimento,
            @RequestParam(required = false) Long quadra,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate data,
            @RequestParam(defaultValue = "quadra") String ordem,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            Model model) {
        var dia = data == null ? LocalDate.now(clock) : data;
        var lista =
                quadras.minhas(usuario.getId()).stream()
                        .filter(
                                q ->
                                        estabelecimento == null
                                                || q.idEstabelecimento() == estabelecimento)
                        .filter(q -> quadra == null || q.id() == quadra)
                        .toList();
        var ids = lista.stream().map(q -> q.id()).collect(java.util.stream.Collectors.toSet());
        var reservas =
                agenda.minhasReservas(usuario, quadra, dia).stream()
                        .filter(r -> ids.contains(r.idQuadra()));
        java.util.Comparator<br.com.agendaplay.model.Reserva> c =
                java.util.Comparator.comparing(r -> r.horaInicio());
        if (!ordem.equals("horario"))
            c =
                    java.util.Comparator.comparing(br.com.agendaplay.model.Reserva::nomeQuadra)
                            .thenComparingLong(br.com.agendaplay.model.Reserva::idQuadra)
                            .thenComparing(c);
        model.addAttribute("reservas", reservas.sorted(c).toList());
        model.addAttribute("quadras", quadras.minhas(usuario.getId()));
        var livres =
                new java.util.LinkedHashMap<
                        br.com.agendaplay.model.Quadra,
                        java.util.List<br.com.agendaplay.model.HorarioLivre>>();
        for (var q : lista)
            livres.put(
                    q,
                    agenda.horariosLivres(q.id()).stream()
                            .filter(h -> h.data().equals(dia))
                            .toList());
        model.addAttribute("livres", livres);
        model.addAttribute("estabelecimentos", estabelecimentos.listar(usuario.getId()));
        model.addAttribute("dia", dia);
        model.addAttribute("filtroEstabelecimento", estabelecimento);
        model.addAttribute("filtroQuadra", quadra);
        model.addAttribute("ordem", ordem);
        return "agenda";
    }

    @PostMapping("/reservas/{id}/cancelar")
    String cancelar(
            @PathVariable long id,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            RedirectAttributes flash) {
        try {
            agenda.cancelar(id, usuario);
            flash.addFlashAttribute("sucesso", "Reserva cancelada. O histórico foi preservado.");
        } catch (RegraNegocioException e) {
            flash.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/reservas";
    }
}
