package com.deverdecasa.web;

import com.deverdecasa.service.RecursoNaoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Transforma falha em página legível.
 *
 * <p>Quem procura o próprio deputado não tem o que fazer com uma stacktrace, então a página de
 * erro diz o que aconteceu e oferece o caminho de volta para a busca. O detalhe técnico vai para
 * o log, onde é útil.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String naoEncontrado(RecursoNaoEncontradoException e, Model model) {
        model.addAttribute("titulo", "Não encontramos essa página");
        model.addAttribute("mensagem", e.getMessage());
        return "erro";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String falhaInesperada(Exception e, Model model) {
        log.error("Falha inesperada ao atender a requisição", e);
        model.addAttribute("titulo", "Algo deu errado do nosso lado");
        model.addAttribute("mensagem",
                "Tivemos um problema ao carregar essa página. Tente de novo em instantes.");
        return "erro";
    }
}
