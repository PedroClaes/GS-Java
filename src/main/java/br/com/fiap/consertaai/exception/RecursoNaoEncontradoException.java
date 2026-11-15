package br.com.fiap.consertaai.exception;

/** 404: o registro pedido nao existe. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String recurso, Object id) {
        super(recurso + " " + id + " não encontrado(a).");
    }
}
