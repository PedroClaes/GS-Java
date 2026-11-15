package br.com.fiap.consertaai.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Publicado na exchange consertaai.eventos (routing key "ocorrencia.criada") quando um
 * cidadao abre uma ocorrencia. O TriagemConsumer o consome e chama a IA fora da requisicao.
 */
public record OcorrenciaCriadaEvento(UUID eventoId, Long ocorrenciaId, Instant dataEvento) {

    public static OcorrenciaCriadaEvento de(Long ocorrenciaId) {
        return new OcorrenciaCriadaEvento(UUID.randomUUID(), ocorrenciaId, Instant.now());
    }
}
