package br.com.agendaplay.controller;

import br.com.agendaplay.dto.QuadraForm;
import br.com.agendaplay.security.UsuarioAutenticado;
import br.com.agendaplay.service.QuadraService;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class QuadraController {
    private final QuadraService quadras;

    public QuadraController(QuadraService quadras) {
        this.quadras = quadras;
    }

    @GetMapping("/cliente/quadras")
    String catalogo(Model model) {
        model.addAttribute("quadras", quadras.ativas());
        model.addAttribute("dono", false);
        return "quadras";
    }

    @GetMapping("/proprietario/quadras")
    String minhas(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        model.addAttribute("quadras", quadras.minhas(usuario.getId()));
        model.addAttribute("dono", true);
        return "quadras";
    }

    @GetMapping("/proprietario/quadras/nova")
    String nova(Model model) {
        model.addAttribute("form", new QuadraForm());
        model.addAttribute("id", null);
        return "quadra-form";
    }

    @GetMapping("/proprietario/quadras/{id}/editar")
    String editar(
            @PathVariable long id,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            Model model) {
        var q = quadras.propria(id, usuario);
        var form = new QuadraForm();
        form.setNome(q.nome());
        form.setModalidade(q.modalidade());
        form.setLocalizacao(q.localizacao());
        form.setValorHora(q.valorHora());
        form.setSituacao(q.situacao());
        model.addAttribute("form", form);
        model.addAttribute("id", id);
        return "quadra-form";
    }

    @PostMapping({"/proprietario/quadras/nova", "/proprietario/quadras/{id}/editar"})
    String salvar(
            @PathVariable(required = false) Long id,
            @Valid @ModelAttribute("form") QuadraForm form,
            BindingResult erros,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            Model model,
            RedirectAttributes flash) {
        if (id != null) quadras.propria(id, usuario);
        model.addAttribute("id", id);
        if (erros.hasErrors()) return "quadra-form";
        quadras.salvar(id, form, usuario);
        flash.addFlashAttribute("sucesso", "Quadra salva com sucesso.");
        return "redirect:/proprietario/quadras";
    }
}
