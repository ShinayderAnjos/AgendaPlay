package br.com.agendaplay.controller;

import br.com.agendaplay.security.UsuarioAutenticado;
import br.com.agendaplay.service.NotificacaoService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
public class DadosComuns {
    private final NotificacaoService notificacoes;

    public DadosComuns(NotificacaoService n) {
        notificacoes = n;
    }

    @ModelAttribute
    public void comuns(@AuthenticationPrincipal UsuarioAutenticado u, Model m) {
        if (u != null) m.addAttribute("notificacoes", notificacoes.pendentes(u.getId()));
    }
}
