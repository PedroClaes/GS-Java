package br.com.fiap.consertaai.domain.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo de vida de uma ocorrencia.
 *
 * <pre>
 * EM_TRIAGEM ──► ABERTA ──► EM_ANDAMENTO ──► RESOLVIDA
 *      │            │              │
 *      └────────────┴──────────────┴──────► CANCELADA
 * </pre>
 *
 * A regra de transicao fica no proprio enum: o service pergunta
 * {@link #podeIrPara(StatusOcorrencia)} em vez de espalhar if/else.
 */
public enum StatusOcorrencia {
    EM_TRIAGEM,
    ABERTA,
    EM_ANDAMENTO,
    RESOLVIDA,
    CANCELADA;

    public Set<StatusOcorrencia> proximosPermitidos() {
        return switch (this) {
            case EM_TRIAGEM -> EnumSet.of(ABERTA, CANCELADA);
            case ABERTA -> EnumSet.of(EM_ANDAMENTO, RESOLVIDA, CANCELADA);
            case EM_ANDAMENTO -> EnumSet.of(RESOLVIDA, CANCELADA);
            case RESOLVIDA, CANCELADA -> EnumSet.noneOf(StatusOcorrencia.class);
        };
    }

    public boolean podeIrPara(StatusOcorrencia destino) {
        return proximosPermitidos().contains(destino);
    }

    /** Ocorrencias ainda em aberto: entram na fila do gestor e podem receber apoios. */
    public boolean emAberto() {
        return this == EM_TRIAGEM || this == ABERTA || this == EM_ANDAMENTO;
    }
}
