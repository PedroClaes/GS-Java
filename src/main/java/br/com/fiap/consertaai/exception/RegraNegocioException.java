package br.com.fiap.consertaai.exception;

/** 422: a requisicao e valida, mas viola uma regra do dominio (ex.: transicao de status proibida). */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
