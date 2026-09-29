package br.com.agendaplay.controller;

import br.com.agendaplay.exception.RegraNegocioException;
import br.com.agendaplay.security.UsuarioAutenticado;
import br.com.agendaplay.service.AvaliacaoService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AvaliacaoController {
    private final AvaliacaoService service;

    public AvaliacaoController(AvaliacaoService service) {
        this.service = service;
    }

    @PostMapping("/proprietario/reservas/{id}/concluir")
    String concluir(
            @PathVariable long id,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            RedirectAttributes flash) {
        try {
            service.concluir(id, usuario);
            flash.addFlashAttribute(
                    "sucesso", "Reserva concluída. O cliente já pode avaliar a quadra.");
        } catch (RegraNegocioException e) {
            flash.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/reservas";
    }

    @PostMapping("/cliente/reservas/{id}/avaliar")
    String avaliar(
            @PathVariable long id,
            @RequestParam int nota,
            @RequestParam(defaultValue = "") String comentario,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            RedirectAttributes flash) {
        try {
            service.avaliar(id, nota, comentario, usuario);
            flash.addFlashAttribute("sucesso", "Obrigado! Sua avaliação foi registrada.");
        } catch (RegraNegocioException e) {
            flash.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/reservas";
    }
}
