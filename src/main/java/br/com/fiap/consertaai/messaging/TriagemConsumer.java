package br.com.fiap.consertaai.messaging;

import br.com.fiap.consertaai.ia.TriagemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor de "ocorrencia.criada": roda a triagem com IA fora da requisicao HTTP.
 *
 * Se lancar excecao, o retry do application.yml tenta de novo (3 vezes, com espera
 * crescente). Esgotadas as tentativas, a mensagem vai para consertaai.java.triagem.dlq.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TriagemConsumer {

    private final TriagemService triagemService;

    @RabbitListener(queues = "${consertaai.rabbit.fila-triagem}")
    public void consumir(OcorrenciaCriadaEvento evento) {
        log.info("Triagem da ocorrencia {} recebida da fila (evento {})", evento.ocorrenciaId(), evento.eventoId());
        triagemService.triar(evento.ocorrenciaId());
    }
}
