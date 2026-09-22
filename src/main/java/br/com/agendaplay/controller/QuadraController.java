package br.com.agendaplay.controller;

import br.com.agendaplay.dto.QuadraForm;
import br.com.agendaplay.security.UsuarioAutenticado;
import br.com.agendaplay.service.*;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.*;
import java.util.Locale;

@Controller
public class QuadraController {
    private final QuadraService quadras;
    private final EstabelecimentoService estabelecimentos;
    private final AgendaService agenda;

    public QuadraController(
            QuadraService quadras, EstabelecimentoService estabelecimentos, AgendaService agenda) {
        this.estabelecimentos = estabelecimentos;
        this.agenda = agenda;
        this.quadras = quadras;
    }

    @GetMapping("/cliente/quadras")
    String catalogo(
            @RequestParam(defaultValue = "") String modalidade,
            @RequestParam(defaultValue = "") String localizacao,
            @RequestParam(required = false) Long estabelecimento,
            @RequestParam(required = false) BigDecimal valorMaximo,
            @RequestParam(required = false)
                    @org.springframework.format.annotation.DateTimeFormat(
                            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                    LocalDate data,
            @RequestParam(required = false) LocalTime inicio,
            @RequestParam(required = false) LocalTime fim,
            @RequestParam(defaultValue = "false") boolean disponiveis,
            Model model) {
        if ((inicio != null || fim != null)
                && (data == null || inicio == null || fim == null || !fim.isAfter(inicio)))
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Informe data e intervalo válido.");
        var lista =
                quadras.ativas().stream()
                        .filter(
                                q ->
                                        q.modalidade()
                                                .toLowerCase(Locale.ROOT)
                                                .contains(modalidade.toLowerCase(Locale.ROOT)))
                        .filter(
                                q ->
                                        q.localizacao()
                                                .toLowerCase(Locale.ROOT)
                                                .contains(localizacao.toLowerCase(Locale.ROOT)))
                        .filter(
                                q ->
                                        estabelecimento == null
                                                || q.idEstabelecimento() == estabelecimento)
                        .filter(
                                q -> {
                                    if (!disponiveis
                                            && data == null
                                            && inicio == null
                                            && valorMaximo == null) return true;
                                    return agenda.horariosLivres(q.id()).stream()
                                            .anyMatch(
                                                    h ->
                                                            (data == null || h.data().equals(data))
                                                                    && (inicio == null
                                                                            || !inicio.isBefore(
                                                                                            h
                                                                                                    .horaInicio())
                                                                                    && !fim.isAfter(
                                                                                            h
                                                                                                    .horaFim()))
                                                                    && (valorMaximo == null
                                                                            || h.valorHora()
                                                                                            .compareTo(
                                                                                                    valorMaximo)
                                                                                    <= 0));
                                })
                        .toList();
        model.addAttribute("estabelecimentos", estabelecimentos.listar(null));
        model.addAttribute(
                "nomesEstabelecimentos",
                estabelecimentos.listar(null).stream()
                        .collect(java.util.stream.Collectors.toMap(e -> e.id(), e -> e.nome())));
        model.addAttribute("filtroEstabelecimento", estabelecimento);
        model.addAttribute("modalidade", modalidade);
        model.addAttribute("localizacao", localizacao);
        model.addAttribute("valorMaximo", valorMaximo);
        model.addAttribute("data", data);
        model.addAttribute("inicio", inicio);
        model.addAttribute("fim", fim);
        model.addAttribute("disponiveis", disponiveis);
        model.addAttribute("quadras", lista);
        model.addAttribute("dono", false);
        return "quadras";
    }

    @GetMapping("/proprietario/quadras")
    String minhas(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        model.addAttribute("quadras", quadras.minhas(usuario.getId()));
        model.addAttribute(
                "nomesEstabelecimentos",
                estabelecimentos.listar(usuario.getId()).stream()
                        .collect(java.util.stream.Collectors.toMap(e -> e.id(), e -> e.nome())));
        model.addAttribute("dono", true);
        return "quadras";
    }

    @GetMapping("/proprietario/quadras/nova")
    String nova(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        model.addAttribute("estabelecimentos", estabelecimentos.listar(usuario.getId()));
        model.addAttribute("form", new QuadraForm());
        model.addAttribute("id", null);
        return "quadra-form";
    }

    @GetMapping("/proprietario/quadras/{id}/editar")
    String editar(
            @PathVariable long id,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            Model model) {
        var q = quadras.propria(id, usuario);
        var form = new QuadraForm();
        form.setNome(q.nome());
        form.setModalidade(q.modalidade());
        form.setLocalizacao(q.localizacao());
        form.setValorHora(q.valorHora());
        form.setSituacao(q.situacao());
        form.setIdEstabelecimento(q.idEstabelecimento());
        form.setToleranciaMinutos(q.toleranciaMinutos());
        form.setLatitude(q.latitude());
        form.setLongitude(q.longitude());
        model.addAttribute("estabelecimentos", estabelecimentos.listar(usuario.getId()));
        model.addAttribute("form", form);
        model.addAttribute("id", id);
        return "quadra-form";
    }

    @PostMapping({"/proprietario/quadras/nova", "/proprietario/quadras/{id}/editar"})
    String salvar(
            @PathVariable(required = false) Long id,
            @Valid @ModelAttribute("form") QuadraForm form,
            BindingResult erros,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            Model model,
            RedirectAttributes flash) {
        if (id != null) quadras.propria(id, usuario);
        model.addAttribute("id", id);
        model.addAttribute("estabelecimentos", estabelecimentos.listar(usuario.getId()));
        if (erros.hasErrors()) return "quadra-form";
        quadras.salvar(id, form, usuario);
        flash.addFlashAttribute("sucesso", "Quadra salva com sucesso.");
        return "redirect:/proprietario/quadras";
    }
}
