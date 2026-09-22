package br.com.agendaplay.controller;

import br.com.agendaplay.dto.PadraoForm;
import br.com.agendaplay.exception.RegraNegocioException;
import br.com.agendaplay.security.UsuarioAutenticado;
import br.com.agendaplay.service.*;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PadraoController {
    private final PadraoService service;
    private final QuadraService quadras;

    public PadraoController(PadraoService p, QuadraService q) {
        service = p;
        quadras = q;
    }

    @GetMapping("/proprietario/quadras/{id}/padroes")
    String form(@PathVariable long id, @AuthenticationPrincipal UsuarioAutenticado u, Model m) {
        var q = quadras.propria(id, u);
        var f = new PadraoForm();
        f.setValorHora(q.valorHora());
        f.setToleranciaMinutos(q.toleranciaMinutos());
        m.addAttribute("form", f);
        carregar(id, u, m);
        return "padroes";
    }

    private void carregar(long id, UsuarioAutenticado u, Model m) {
        m.addAttribute("quadra", quadras.propria(id, u));
        m.addAttribute("padroes", service.listar(id));
    }

    @PostMapping("/proprietario/quadras/{id}/padroes")
    String criar(
            @PathVariable long id,
            @Valid @ModelAttribute("form") PadraoForm f,
            BindingResult erros,
            @AuthenticationPrincipal UsuarioAutenticado u,
            Model m,
            RedirectAttributes flash) {
        carregar(id, u, m);
        if (!erros.hasErrors())
            try {
                service.criar(id, f, u);
                flash.addFlashAttribute(
                        "sucesso",
                        "Padrão criado. Agenda gerada até o fim do mês seguinte; renovação"
                            + " automática enquanto ativo.");
                return "redirect:/proprietario/quadras/" + id + "/padroes";
            } catch (RegraNegocioException e) {
                erros.reject("padrao", e.getMessage());
            }
        return "padroes";
    }

    @PostMapping("/proprietario/quadras/{id}/padroes/{padrao}/desativar")
    String desativar(
            @PathVariable long id,
            @PathVariable long padrao,
            @AuthenticationPrincipal UsuarioAutenticado u,
            RedirectAttributes flash) {
        service.desativar(id, padrao, u);
        flash.addFlashAttribute(
                "sucesso", "Padrão desativado. Reservas confirmadas foram preservadas.");
        return "redirect:/proprietario/quadras/" + id + "/padroes";
    }
}
