package br.com.agendaplay.controller;

import br.com.agendaplay.exception.RegraNegocioException;
import br.com.agendaplay.security.UsuarioAutenticado;
import br.com.agendaplay.service.*;

import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class FotoController {
    private final FotoService fotos;
    private final QuadraService quadras;

    public FotoController(FotoService fotos, QuadraService quadras) {
        this.fotos = fotos;
        this.quadras = quadras;
    }

    @GetMapping("/fotos/{id}")
    ResponseEntity<byte[]> imagem(
            @PathVariable long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noStore())
                .body(fotos.conteudo(id, usuario));
    }

    @GetMapping("/proprietario/quadras/{id}/fotos")
    String gerenciar(
            @PathVariable long id,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            Model model) {
        model.addAttribute("quadra", quadras.propria(id, usuario));
        model.addAttribute("fotos", fotos.listar(id));
        return "fotos";
    }

    @PostMapping("/proprietario/quadras/{id}/fotos")
    String adicionar(
            @PathVariable long id,
            @RequestParam(required = false) List<MultipartFile> arquivos,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            RedirectAttributes flash) {
        try {
            fotos.adicionar(id, arquivos, usuario);
            flash.addFlashAttribute("sucesso", "Fotos salvas.");
        } catch (RegraNegocioException e) {
            flash.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/proprietario/quadras/" + id + "/fotos";
    }

    @PostMapping("/proprietario/quadras/{id}/fotos/{foto}/{acao}")
    String alterar(
            @PathVariable long id,
            @PathVariable long foto,
            @PathVariable String acao,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!acao.equals("excluir") && !acao.equals("capa"))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND);
        fotos.alterar(id, foto, acao.equals("excluir"), usuario);
        return "redirect:/proprietario/quadras/" + id + "/fotos";
    }
}
