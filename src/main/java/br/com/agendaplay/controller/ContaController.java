package br.com.agendaplay.controller;

import br.com.agendaplay.dto.CadastroForm;
import br.com.agendaplay.exception.RegraNegocioException;
import br.com.agendaplay.model.Perfil;
import br.com.agendaplay.repository.UsuarioRepository;
import br.com.agendaplay.security.UsuarioAutenticado;
import br.com.agendaplay.service.UsuarioService;

import jakarta.validation.Valid;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class ContaController {
    private final UsuarioService service;
    private final UsuarioRepository usuarios;

    public ContaController(UsuarioService service, UsuarioRepository usuarios) {
        this.service = service;
        this.usuarios = usuarios;
    }

    @GetMapping("/")
    String inicio() {
        return "inicio";
    }

    @GetMapping("/login")
    String login() {
        return "login";
    }

    @GetMapping("/painel")
    String painel(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return usuario.getPerfil() == Perfil.PROPRIETARIO
                ? "redirect:/proprietario/quadras"
                : "redirect:/cliente/quadras";
    }

    private Perfil perfil(String tipo) {
        return switch (tipo) {
            case "cliente" -> Perfil.CLIENTE;
            case "proprietario" -> Perfil.PROPRIETARIO;
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        };
    }

    @GetMapping("/cadastro/{tipo}")
    String cadastro(@PathVariable String tipo, Model model) {
        model.addAttribute("tipo", tipo);
        model.addAttribute("perfil", perfil(tipo));
        model.addAttribute("form", new CadastroForm());
        return "cadastro";
    }

    @PostMapping("/cadastro/{tipo}")
    String cadastrar(
            @PathVariable String tipo,
            @Valid @ModelAttribute("form") CadastroForm form,
            BindingResult erros,
            Model model) {
        var papel = perfil(tipo);
        model.addAttribute("tipo", tipo);
        model.addAttribute("perfil", papel);
        if (!erros.hasErrors()) {
            try {
                service.cadastrar(form, papel);
                return "redirect:/login?cadastrado";
            } catch (RegraNegocioException e) {
                erros.reject("cadastro", e.getMessage());
            } catch (DataIntegrityViolationException e) {
                erros.reject(
                        "duplicado",
                        "E-mail ou CPF/CNPJ já cadastrado. Confira os dados ou entre na sua"
                            + " conta.");
            }
        }
        form.setSenha(null);
        form.setConfirmacaoSenha(null);
        return "cadastro";
    }

    @GetMapping("/conta")
    String conta(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        model.addAttribute("conta", usuarios.buscarPorEmail(usuario.getUsername()).orElseThrow());
        return "conta";
    }

    @GetMapping("/acesso-negado")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    String proibido(Model model) {
        model.addAttribute("mensagem", "Seu perfil não tem acesso a esta função.");
        return "error";
    }
}
