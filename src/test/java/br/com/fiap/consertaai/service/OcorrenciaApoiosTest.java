package br.com.fiap.consertaai.service;

import br.com.fiap.consertaai.domain.entity.Ocorrencia;
import br.com.fiap.consertaai.domain.enums.StatusOcorrencia;
import br.com.fiap.consertaai.domain.enums.Urgencia;
import br.com.fiap.consertaai.repository.CategoriaRepository;
import br.com.fiap.consertaai.repository.HistoricoStatusRepository;
import br.com.fiap.consertaai.repository.OcorrenciaRepository;
import br.com.fiap.consertaai.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Regras do consumo dos eventos de apoio vindos da API .NET. */
class OcorrenciaApoiosTest {

    private final OcorrenciaRepository ocorrenciaRepository = mock(OcorrenciaRepository.class);
    private final OcorrenciaService service = new OcorrenciaService(ocorrenciaRepository,
            mock(CategoriaRepository.class), mock(UsuarioRepository.class), mock(HistoricoStatusRepository.class),
            new PrioridadeCalculator(), mock(ApplicationEventPublisher.class));

    private Ocorrencia ocorrencia() {
        return Ocorrencia.builder().id(1L).status(StatusOcorrencia.ABERTA).urgencia(Urgencia.MEDIA)
                .qtdApoios(2).criadoEm(LocalDateTime.now()).build();
    }

    @Test
    void aplicaOTotalERecalculaAPrioridade() {
        Ocorrencia o = ocorrencia();
        when(ocorrenciaRepository.findById(1L)).thenReturn(Optional.of(o));

        boolean aplicado = service.atualizarApoios(1L, 6, Instant.now());

        assertThat(aplicado).isTrue();
        assertThat(o.getQtdApoios()).isEqualTo(6);
        assertThat(o.getPrioridade()).isEqualTo(30 + 12);
    }

    @Test
    void mesmoEventoDuasVezesDaOMesmoResultado() {
        Ocorrencia o = ocorrencia();
        when(ocorrenciaRepository.findById(1L)).thenReturn(Optional.of(o));
        Instant quando = Instant.now();

        service.atualizarApoios(1L, 6, quando);
        service.atualizarApoios(1L, 6, quando);

        assertThat(o.getQtdApoios()).isEqualTo(6);
    }

    @Test
    void eventoAntigoQueChegaAtrasadoEDescartado() {
        Ocorrencia o = ocorrencia();
        Instant recente = Instant.now();
        o.setQtdApoios(6);
        o.setUltimoEventoApoioEm(LocalDateTime.ofInstant(recente, ZoneOffset.UTC));
        when(ocorrenciaRepository.findById(1L)).thenReturn(Optional.of(o));

        boolean aplicado = service.atualizarApoios(1L, 4, recente.minusSeconds(30));

        assertThat(aplicado).isFalse();
        assertThat(o.getQtdApoios()).isEqualTo(6);
    }

    @Test
    void ocorrenciaInexistenteNaoQuebraOConsumidor() {
        when(ocorrenciaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.atualizarApoios(99L, 3, Instant.now())).isFalse();
    }
}
