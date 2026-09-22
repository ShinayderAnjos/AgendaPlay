package br.com.agendaplay.controller;

import br.com.agendaplay.security.UsuarioAutenticado;
import br.com.agendaplay.service.NotificacaoService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class NotificacaoController {
    private final NotificacaoService service;

    public NotificacaoController(NotificacaoService s) {
        service = s;
    }

    @PostMapping("/notificacoes/{id}/ler")
    public String ler(@PathVariable long id, @AuthenticationPrincipal UsuarioAutenticado u) {
        service.ler(id, u.getId());
        return "redirect:/painel";
    }
}
