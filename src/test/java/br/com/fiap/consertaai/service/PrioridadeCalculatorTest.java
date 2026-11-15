package br.com.fiap.consertaai.service;

import br.com.fiap.consertaai.domain.enums.Urgencia;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PrioridadeCalculatorTest {

    private final PrioridadeCalculator calculator = new PrioridadeCalculator();
    private final LocalDateTime agora = LocalDateTime.of(2026, 11, 15, 12, 0);

    @Test
    void somaUrgenciaRiscoApoiosEDias() {
        // ALTA 50 + risco 15 + 7 apoios (14) + 5 dias (5) = 84
        int p = calculator.calcular(Urgencia.ALTA, true, 7, agora.minusDays(5), agora);
        assertThat(p).isEqualTo(84);
    }

    @Test
    void apoiosEDiasTemTeto() {
        int p = calculator.calcular(Urgencia.BAIXA, false, 1000, agora.minusDays(300), agora);
        assertThat(p).isEqualTo(15 + 15 + 5);
    }

    @Test
    void nuncaPassaDeCem() {
        int p = calculator.calcular(Urgencia.CRITICA, true, 1000, agora.minusDays(300), agora);
        assertThat(p).isEqualTo(100);
    }

    @Test
    void semTriagemUsaPesoNeutro() {
        assertThat(calculator.calcular(null, false, 0, agora, agora)).isEqualTo(20);
    }

    @Test
    void maisApoiosSobemNaFila() {
        int poucos = calculator.calcular(Urgencia.MEDIA, false, 1, agora, agora);
        int muitos = calculator.calcular(Urgencia.MEDIA, false, 6, agora, agora);
        assertThat(muitos).isGreaterThan(poucos);
    }
}
