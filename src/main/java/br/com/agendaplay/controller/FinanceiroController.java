package br.com.agendaplay.controller;
import br.com.agendaplay.exception.RegraNegocioException;
import br.com.agendaplay.security.UsuarioAutenticado;
import br.com.agendaplay.service.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import java.util.stream.Collectors;
@Controller
public class FinanceiroController {
    private final FinanceiroService financeiro;
    private final CaixaService caixas;
    public FinanceiroController(FinanceiroService financeiro,CaixaService caixas) {
        this.financeiro=financeiro; this.caixas=caixas;
    }
    @GetMapping({"/proprietario/financeiro","/cliente/financeiro"})
    String listar(@RequestParam(required=false) String mes,@AuthenticationPrincipal UsuarioAutenticado u,Model model) {
        YearMonth periodo=null;
        if(mes!=null && !mes.isBlank()) {
            try { periodo=YearMonth.parse(mes); }
            catch(DateTimeParseException e) { model.addAttribute("erro","Informe um mês válido."); }
        }
        boolean cliente=u.getPerfil()==br.com.agendaplay.model.Perfil.CLIENTE;
        model.addAttribute("clienteFinanceiro",cliente);
        model.addAttribute("rotaFinanceiro",cliente?"/cliente/financeiro":"/proprietario/financeiro");
        model.addAttribute("mes",periodo);
        model.addAttribute("resumo",cliente?financeiro.consultarCliente(u,periodo):financeiro.consultar(u,periodo));
        if(!cliente) model.addAttribute("caixasAbertos",caixas.listar(u).stream().filter(c->c.fechadoEm()==null)
            .map(br.com.agendaplay.model.Caixa::idEstabelecimento).collect(Collectors.toSet()));
        return "financeiro";
    }
    @PostMapping("/proprietario/financeiro/{id}/receber")
    String receber(@PathVariable long id,@RequestParam String forma,@RequestParam String valor,
            @RequestParam String chave,@AuthenticationPrincipal UsuarioAutenticado u,RedirectAttributes flash) {
        try {
            financeiro.registrar(id,forma,CaixaController.valor(valor),UUID.fromString(chave),u);
            flash.addFlashAttribute("sucesso","Recebimento registrado. O saldo restante foi atualizado.");
        } catch(RegraNegocioException e) { flash.addFlashAttribute("erro",e.getMessage()); }
        catch(IllegalArgumentException e) { flash.addFlashAttribute("erro","Recarregue a página e confira os dados da operação."); }
        return "redirect:/proprietario/financeiro";
    }
    @PostMapping("/proprietario/financeiro/{id}/estornar")
    String estornar(@PathVariable long id,@AuthenticationPrincipal UsuarioAutenticado u,RedirectAttributes flash) {
        try { financeiro.registrarEstorno(id,u); flash.addFlashAttribute("sucesso","Estorno manual registrado."); }
        catch(RegraNegocioException e) { flash.addFlashAttribute("erro",e.getMessage()); }
        return "redirect:/proprietario/financeiro";
    }
}
