package br.com.fiap.consertaai.domain;

import br.com.fiap.consertaai.domain.enums.StatusOcorrencia;
import org.junit.jupiter.api.Test;

import static br.com.fiap.consertaai.domain.enums.StatusOcorrencia.*;
import static org.assertj.core.api.Assertions.assertThat;

class StatusOcorrenciaTest {

    @Test
    void cicloNormalDeAtendimento() {
        assertThat(EM_TRIAGEM.podeIrPara(ABERTA)).isTrue();
        assertThat(ABERTA.podeIrPara(EM_ANDAMENTO)).isTrue();
        assertThat(EM_ANDAMENTO.podeIrPara(RESOLVIDA)).isTrue();
    }

    @Test
    void estadosFinaisNaoMudamMais() {
        for (StatusOcorrencia destino : StatusOcorrencia.values()) {
            assertThat(RESOLVIDA.podeIrPara(destino)).isFalse();
            assertThat(CANCELADA.podeIrPara(destino)).isFalse();
        }
    }

    @Test
    void naoVoltaParaTriagemNemPulaTriagemParaAndamento() {
        assertThat(ABERTA.podeIrPara(EM_TRIAGEM)).isFalse();
        assertThat(EM_TRIAGEM.podeIrPara(EM_ANDAMENTO)).isFalse();
    }
}
