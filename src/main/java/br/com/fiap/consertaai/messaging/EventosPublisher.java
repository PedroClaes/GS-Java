package br.com.fiap.consertaai.messaging;

import br.com.fiap.consertaai.ia.TriagemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Produtor: leva o evento de "ocorrencia criada" ao RabbitMQ.
 *
 * AFTER_COMMIT: so publica se a ocorrencia foi realmente gravada. Publicar antes do
 * commit faria o consumidor procurar uma ocorrencia que ainda (ou nunca) existe.
 *
 * @Async: roda em outra thread; o cidadao recebe o 201 sem esperar o broker.
 *
 * Fallback: se o broker estiver fora do ar, a triagem roda direto, nesta mesma thread
 * assincrona. A ocorrencia nunca fica presa em EM_TRIAGEM por falha de mensageria.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventosPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final TriagemService triagemService;

    @Value("${consertaai.rabbit.exchange}")
    private String exchange;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void ocorrenciaCriada(OcorrenciaCriadaEvento evento) {
        try {
            rabbitTemplate.convertAndSend(exchange, RabbitConfig.ROUTING_OCORRENCIA_CRIADA, evento);
            log.info("Evento ocorrencia.criada publicado para a ocorrencia {}", evento.ocorrenciaId());
        } catch (AmqpException ex) {
            log.warn("RabbitMQ indisponivel ({}); triagem da ocorrencia {} feita sem fila",
                    ex.getMessage(), evento.ocorrenciaId());
            triagemService.triar(evento.ocorrenciaId());
        }
    }
}
