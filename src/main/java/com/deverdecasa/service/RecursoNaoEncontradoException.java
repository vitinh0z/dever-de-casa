package com.deverdecasa.service;

/** Pedido de um parlamentar ou proposição que não existe no banco. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    public static RecursoNaoEncontradoException parlamentar(Long id) {
        return new RecursoNaoEncontradoException("Não encontramos parlamentar com o identificador " + id + ".");
    }
}
