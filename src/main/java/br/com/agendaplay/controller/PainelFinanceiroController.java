package br.com.agendaplay.controller;

import br.com.agendaplay.exception.RegraNegocioException;
import br.com.agendaplay.security.UsuarioAutenticado;
import br.com.agendaplay.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;

@Controller
public class PainelFinanceiroController {
    private final PainelFinanceiroService painel;
    private final EstabelecimentoService estabelecimentos;
    public PainelFinanceiroController(PainelFinanceiroService painel,EstabelecimentoService estabelecimentos) {
        this.painel=painel; this.estabelecimentos=estabelecimentos;
    }
    @GetMapping("/proprietario/painel-financeiro")
    String mostrar(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required=false) Long estabelecimento,@AuthenticationPrincipal UsuarioAutenticado u,Model model) {
        model.addAttribute("resumo",painel.consultar(inicio,fim,estabelecimento,u));
        model.addAttribute("estabelecimentos",estabelecimentos.listar(u.getId()));
        model.addAttribute("inicio",inicio); model.addAttribute("fim",fim); model.addAttribute("filtroEstabelecimento",estabelecimento);
        return "painel-financeiro";
    }
    @PostMapping("/proprietario/despesas")
    String despesa(@RequestParam long estabelecimento,@RequestParam String descricao,@RequestParam String valor,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate data,
            @AuthenticationPrincipal UsuarioAutenticado u,RedirectAttributes flash) {
        try { painel.despesa(estabelecimento,descricao,CaixaController.valor(valor),data,u);
            flash.addFlashAttribute("sucesso","Despesa registrada."); }
        catch(RegraNegocioException | IllegalArgumentException e) { flash.addFlashAttribute("erro",e.getMessage()); }
        return "redirect:/proprietario/painel-financeiro";
    }
}
