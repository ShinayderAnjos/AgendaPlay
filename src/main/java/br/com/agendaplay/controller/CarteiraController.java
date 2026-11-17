package br.com.agendaplay.controller;

import br.com.agendaplay.exception.RegraNegocioException;
import br.com.agendaplay.security.UsuarioAutenticado;
import br.com.agendaplay.service.CarteiraService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.UUID;

@Controller
public class CarteiraController {
    private final CarteiraService carteira;
    public CarteiraController(CarteiraService carteira) { this.carteira=carteira; }
    @GetMapping("/cliente/carteira")
    String mostrar(@AuthenticationPrincipal UsuarioAutenticado u,Model model) {
        model.addAttribute("saldo",carteira.saldo(u));
        model.addAttribute("movimentos",carteira.historico(u));
        model.addAttribute("chave",UUID.randomUUID());
        return "carteira";
    }
    @PostMapping("/cliente/carteira/recarregar")
    String recarregar(@RequestParam String valor,@RequestParam String chave,
            @AuthenticationPrincipal UsuarioAutenticado u,RedirectAttributes flash) {
        try {
            carteira.recarregar(CaixaController.valor(valor),UUID.fromString(chave),u);
            flash.addFlashAttribute("sucesso","Recarga simulada registrada. Nenhum dinheiro real foi movimentado.");
        } catch(RegraNegocioException | IllegalArgumentException e) { flash.addFlashAttribute("erro",e.getMessage()); }
        return "redirect:/cliente/carteira";
    }
    @PostMapping("/cliente/carteira/reservas/{id}/pagar")
    String pagar(@PathVariable long id,@AuthenticationPrincipal UsuarioAutenticado u,RedirectAttributes flash) {
        try { carteira.pagar(id,u); flash.addFlashAttribute("sucesso","Pagamento com carteira registrado."); }
        catch(RegraNegocioException e) { flash.addFlashAttribute("erro",e.getMessage()); }
        return "redirect:/reservas";
    }
}
