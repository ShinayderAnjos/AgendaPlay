package br.com.agendaplay.controller;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class ErrosController {
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public String fotoGrande(Model model) {
        model.addAttribute(
                "mensagem",
                "As fotos excedem o limite permitido. Use até 8 imagens de até 5 MB cada e tente"
                    + " novamente.");
        return "error";
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String formatoInvalido(Model model) {
        model.addAttribute(
                "mensagem",
                "Um dos valores informados é inválido. Confira os campos e tente novamente.");
        return "error";
    }
}
