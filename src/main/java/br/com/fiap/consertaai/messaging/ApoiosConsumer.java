package br.com.fiap.consertaai.messaging;

import br.com.fiap.consertaai.service.OcorrenciaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor dos eventos de apoio publicados pela API .NET.
 *
 * Quando um cidadao apoia uma ocorrencia na API .NET, o total atualizado chega aqui e
 * a prioridade da ocorrencia e recalculada: problemas com mais apoios sobem na fila do gestor.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApoiosConsumer {

    private final OcorrenciaService ocorrenciaService;

    @RabbitListener(queues = "${consertaai.rabbit.fila-apoios}")
    public void consumir(ApoioAlteradoEvento evento) {
        if (evento.ocorrenciaId() == null || evento.totalApoios() == null) {
            log.warn("Evento de apoio incompleto descartado: {}", evento);
            return;
        }
        boolean aplicado = ocorrenciaService.atualizarApoios(
                evento.ocorrenciaId(), evento.totalApoios(), evento.dataEvento());
        log.info("Evento {} da ocorrencia {} (total {}): {}", evento.tipo(), evento.ocorrenciaId(),
                evento.totalApoios(), aplicado ? "aplicado" : "descartado");
    }
}
