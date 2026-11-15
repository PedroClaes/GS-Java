package br.com.fiap.consertaai.ia;

import br.com.fiap.consertaai.domain.enums.OrigemTriagem;
import br.com.fiap.consertaai.domain.enums.Urgencia;

/** Resultado normalizado da triagem, venha da IA ou das regras de fallback. */
public record ResultadoTriagem(
        String categoria,
        Urgencia urgencia,
        boolean riscoImediato,
        String orgaoSugerido,
        String resumo,
        Long possivelDuplicadaDeId,
        OrigemTriagem origem) {}
