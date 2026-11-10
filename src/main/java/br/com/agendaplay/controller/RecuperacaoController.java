package br.com.agendaplay.controller;
import br.com.agendaplay.exception.RegraNegocioException;
import br.com.agendaplay.service.RecuperacaoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
public class RecuperacaoController {
    private final RecuperacaoService service;
    private static final String PERMISSAO="redefinicaoValidada";
    public RecuperacaoController(RecuperacaoService service) { this.service=service; }
    @GetMapping("/recuperar-senha")
    String solicitar() { return "recuperar-senha"; }
    @PostMapping("/recuperar-senha")
    String enviar(@RequestParam String email,Model model,HttpSession session) {
        session.removeAttribute(PERMISSAO);
        try { service.solicitar(email); }
        catch(MailException e) {
            org.slf4j.LoggerFactory.getLogger(getClass())
                .warn("Não foi possível entregar e-mail de recuperação. Confira a configuração SMTP.");
        }
        model.addAttribute("email",email);
        model.addAttribute("sucesso","Se o e-mail estiver cadastrado, enviaremos um código. Confira também o spam. Aguarde um minuto antes de solicitar novamente.");
        return "validar-codigo";
    }
    @GetMapping("/validar-codigo")
    String codigo() { return "validar-codigo"; }
    @PostMapping("/validar-codigo")
    String validar(@RequestParam String email,@RequestParam String codigo,HttpSession session,Model model) {
        session.removeAttribute(PERMISSAO);
        try {
            String permissao=service.validarCodigo(email,codigo);
            session.setAttribute(PERMISSAO,permissao);
            return "redirect:/redefinir-senha";
        } catch(RegraNegocioException e) {
            model.addAttribute("email",email); model.addAttribute("erro",e.getMessage());
            return "validar-codigo";
        }
    }
    private String permissao(String token,HttpSession session) {
        // Links emitidos antes desta sprint continuam válidos até a expiração.
        return token!=null && !token.isBlank()?token:(String)session.getAttribute(PERMISSAO);
    }
    @GetMapping("/redefinir-senha")
    String formulario(@RequestParam(required=false) String token,HttpSession session,Model model) {
        model.addAttribute("token",token==null?"":token);
        model.addAttribute("valido",service.valido(permissao(token,session)));
        return "redefinir-senha";
    }
    @PostMapping("/redefinir-senha")
    String redefinir(@RequestParam(required=false) String token,@RequestParam String senha,
            @RequestParam String confirmacaoSenha,HttpSession session,Model model) {
        try {
            service.redefinir(permissao(token,session),senha,confirmacaoSenha);
            session.removeAttribute(PERMISSAO);
            return "redirect:/login?redefinida";
        } catch(RegraNegocioException e) {
            model.addAttribute("erro",e.getMessage()); model.addAttribute("token",token==null?"":token);
            model.addAttribute("valido",service.valido(permissao(token,session)));
            return "redefinir-senha";
        }
    }
}
