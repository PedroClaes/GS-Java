package br.com.fiap.consertaai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Formato unico de erro de toda a API. O app Mobile trata qualquer falha lendo
 * "mensagem" e, em erros de validacao, a lista "campos".
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErroResponse(
        Instant timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho,
        List<CampoInvalido> campos) {

    public record CampoInvalido(String campo, String mensagem) {}

    public static ErroResponse de(int status, String erro, String mensagem, String caminho) {
        return new ErroResponse(Instant.now(), status, erro, mensagem, caminho, List.of());
    }
}
