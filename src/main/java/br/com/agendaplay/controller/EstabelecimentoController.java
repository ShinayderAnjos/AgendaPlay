package br.com.agendaplay.controller;

import br.com.agendaplay.dto.EstabelecimentoForm;
import br.com.agendaplay.model.Perfil;
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
public class EstabelecimentoController {
    private final EstabelecimentoService service;

    public EstabelecimentoController(EstabelecimentoService s) {
        service = s;
    }

    @GetMapping({"/proprietario/estabelecimentos", "/cliente/estabelecimentos"})
    String lista(@AuthenticationPrincipal UsuarioAutenticado u, Model m) {
        boolean dono = u.getPerfil() == Perfil.PROPRIETARIO;
        m.addAttribute("dono", dono);
        m.addAttribute("estabelecimentos", service.listar(dono ? u.getId() : null));
        return "estabelecimentos";
    }

    @GetMapping({
        "/proprietario/estabelecimentos/novo",
        "/proprietario/estabelecimentos/{id}/editar"
    })
    String form(
            @PathVariable(required = false) Long id,
            @AuthenticationPrincipal UsuarioAutenticado u,
            Model m) {
        var f = new EstabelecimentoForm();
        if (id != null) {
            var e = service.propria(id, u);
            f.setNome(e.nome());
            f.setLocalizacao(e.localizacao());
            f.setLatitude(e.latitude());
            f.setLongitude(e.longitude());
        }
        m.addAttribute("form", f);
        m.addAttribute("id", id);
        return "estabelecimento-form";
    }

    @PostMapping({
        "/proprietario/estabelecimentos/novo",
        "/proprietario/estabelecimentos/{id}/editar"
    })
    String salvar(
            @PathVariable(required = false) Long id,
            @Valid @ModelAttribute("form") EstabelecimentoForm f,
            BindingResult erros,
            @AuthenticationPrincipal UsuarioAutenticado u,
            Model m,
            RedirectAttributes flash) {
        if (id != null) service.propria(id, u);
        m.addAttribute("id", id);
        if (erros.hasErrors()) return "estabelecimento-form";
        service.salvar(id, f, u);
        flash.addFlashAttribute("sucesso", "Estabelecimento salvo.");
        return "redirect:/proprietario/estabelecimentos";
    }
}
