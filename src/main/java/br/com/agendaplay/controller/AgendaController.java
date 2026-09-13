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

    public AgendaController(AgendaService agenda, QuadraService quadras) {
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
