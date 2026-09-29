package br.com.agendaplay.controller;

import br.com.agendaplay.exception.RegraNegocioException;
import br.com.agendaplay.service.RecuperacaoService;

import org.springframework.mail.MailException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class RecuperacaoController {
    private final RecuperacaoService service;

    public RecuperacaoController(RecuperacaoService service) {
        this.service = service;
    }

    @GetMapping("/recuperar-senha")
    String solicitar() {
        return "recuperar-senha";
    }

    @PostMapping("/recuperar-senha")
    String enviar(@RequestParam String email, Model model) {
        try {
            service.solicitar(email);
        } catch (MailException e) {
            org.slf4j.LoggerFactory.getLogger(getClass())
                    .warn(
                            "Não foi possível entregar e-mail de recuperação. Confira a"
                                + " configuração SMTP.");
        }
        model.addAttribute(
                "sucesso",
                "Se o e-mail estiver cadastrado, enviaremos um link de recuperação. Confira também"
                    + " a pasta de spam.");
        return "recuperar-senha";
    }

    @GetMapping("/redefinir-senha")
    String formulario(@RequestParam(defaultValue = "") String token, Model model) {
        model.addAttribute("token", token);
        model.addAttribute("valido", service.valido(token));
        return "redefinir-senha";
    }

    @PostMapping("/redefinir-senha")
    String redefinir(
            @RequestParam String token,
            @RequestParam String senha,
            @RequestParam String confirmacaoSenha,
            Model model) {
        try {
            service.redefinir(token, senha, confirmacaoSenha);
            return "redirect:/login?redefinida";
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            model.addAttribute("token", token);
            model.addAttribute("valido", service.valido(token));
            return "redefinir-senha";
        }
    }
}
