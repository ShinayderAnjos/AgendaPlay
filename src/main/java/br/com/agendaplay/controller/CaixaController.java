package br.com.agendaplay.controller;
import br.com.agendaplay.exception.RegraNegocioException;
import br.com.agendaplay.security.UsuarioAutenticado;
import br.com.agendaplay.service.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
@Controller
@RequestMapping("/proprietario/caixa")
public class CaixaController {
    private final CaixaService caixas;
    private final EstabelecimentoService estabelecimentos;
    public CaixaController(CaixaService caixas,EstabelecimentoService estabelecimentos) {
        this.caixas=caixas; this.estabelecimentos=estabelecimentos;
    }
    static BigDecimal valor(String texto) {
        if(texto==null || !texto.matches("[0-9]{1,10}([.,][0-9]{1,2})?"))
            throw new RegraNegocioException("Informe um valor válido em reais, com até duas casas decimais.");
        return new BigDecimal(texto.replace(',','.'));
    }
    @GetMapping
    String listar(@AuthenticationPrincipal UsuarioAutenticado u,Model model) {
        model.addAttribute("estabelecimentos",estabelecimentos.listar(u.getId()));
        model.addAttribute("caixas",caixas.listar(u));
        return "caixas";
    }
    @PostMapping("/abrir")
    String abrir(@RequestParam long estabelecimento,@RequestParam String saldoInicial,
            @AuthenticationPrincipal UsuarioAutenticado u,RedirectAttributes flash) {
        try {
            long id=caixas.abrir(estabelecimento,valor(saldoInicial),u);
            flash.addFlashAttribute("sucesso","Caixa aberto.");
            return "redirect:/proprietario/caixa/"+id;
        } catch(RegraNegocioException e) { flash.addFlashAttribute("erro",e.getMessage()); }
        return "redirect:/proprietario/caixa";
    }
    @GetMapping("/{id}")
    String detalhe(@PathVariable long id,@AuthenticationPrincipal UsuarioAutenticado u,Model model) {
        model.addAttribute("resumoCaixa",caixas.resumo(id,u));
        model.addAttribute("movimentos",caixas.movimentos(id,u));
        return "caixa-detalhe";
    }
    @PostMapping("/{id}/fechar")
    String fechar(@PathVariable long id,@RequestParam String saldoInformado,
            @RequestParam(defaultValue="") String observacao,@AuthenticationPrincipal UsuarioAutenticado u,
            RedirectAttributes flash) {
        try { caixas.fechar(id,valor(saldoInformado),observacao,u); flash.addFlashAttribute("sucesso","Caixa fechado. Conferência salva."); }
        catch(RegraNegocioException e) { flash.addFlashAttribute("erro",e.getMessage()); }
        return "redirect:/proprietario/caixa/"+id;
    }
}
