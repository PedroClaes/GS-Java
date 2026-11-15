package br.com.fiap.consertaai.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento publicado pela API .NET (routing keys "apoio.registrado" e "apoio.removido").
 * Os nomes dos campos seguem o JSON camelCase que o .NET serializa.
 *
 * Carrega o TOTAL de apoios, e nao "+1/-1": aplicar o mesmo evento duas vezes da o mesmo
 * resultado (idempotente), e dataEvento permite descartar um evento antigo que chegue atrasado.
 */
public record ApoioAlteradoEvento(
        UUID eventoId,
        String tipo,
        Long ocorrenciaId,
        String usuarioId,
        Integer totalApoios,
        Instant dataEvento) {}
