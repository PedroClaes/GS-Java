package br.com.fiap.consertaai.ia;

import java.util.List;

/**
 * Fotografia dos dados que a triagem precisa, montada dentro de uma transacao curta.
 * A chamada ao modelo de IA (lenta) acontece fora da transacao, com estes dados em memoria.
 */
public record TriagemEntrada(
        Long ocorrenciaId,
        String titulo,
        String descricao,
        String bairro,
        String categoriaInformada,
        List<CategoriaOpcao> categorias,
        List<Candidata> candidatas) {

    public record CategoriaOpcao(String nome, String descricao, String orgaoResponsavel) {}

    /** Ocorrencia em aberto no mesmo bairro, comparada para detectar duplicidade. */
    public record Candidata(Long id, String titulo, String descricao, String categoria) {}
}
