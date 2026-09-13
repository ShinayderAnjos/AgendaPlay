package br.com.agendaplay.controller;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class ErrosController {
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String formatoInvalido(Model model) {
        model.addAttribute(
                "mensagem",
                "Um dos valores informados é inválido. Confira os campos e tente novamente.");
        return "error";
    }
}
