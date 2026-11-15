package br.com.fiap.consertaai.exception;

/** 409: conflito com um dado ja existente (ex.: e-mail ja cadastrado). */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
