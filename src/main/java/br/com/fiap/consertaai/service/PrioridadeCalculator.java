package br.com.fiap.consertaai.service;

import br.com.fiap.consertaai.domain.entity.Ocorrencia;
import br.com.fiap.consertaai.domain.enums.Urgencia;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Prioridade da fila do gestor, de 0 a 100. Regra deterministica e explicavel:
 *
 * <pre>
 *   urgencia (IA)        BAIXA 15 | MEDIA 30 | ALTA 50 | CRITICA 65
 * + risco imediato       +15
 * + apoios dos cidadaos  +2 por apoio, ate +15   (vem da API .NET)
 * + tempo em aberto      +1 por dia, ate +5      (nada fica esquecido)
 * </pre>
 *
 * A IA sugere urgencia e risco; quem ordena a fila e esta conta, que o gestor consegue auditar.
 */
@Component
public class PrioridadeCalculator {

    static final int SEM_URGENCIA = 20;
    static final int BONUS_RISCO = 15;
    static final int PONTOS_POR_APOIO = 2;
    static final int MAX_APOIOS = 15;
    static final int MAX_DIAS = 5;

    public int calcular(Urgencia urgencia, boolean riscoImediato, int apoios,
                        LocalDateTime criadoEm, LocalDateTime agora) {
        int pontos = urgencia == null ? SEM_URGENCIA : urgencia.getPeso();
        if (riscoImediato) {
            pontos += BONUS_RISCO;
        }
        pontos += Math.min(Math.max(apoios, 0) * PONTOS_POR_APOIO, MAX_APOIOS);
        if (criadoEm != null) {
            long dias = Math.max(Duration.between(criadoEm, agora).toDays(), 0);
            pontos += (int) Math.min(dias, MAX_DIAS);
        }
        return Math.min(pontos, 100);
    }

    public void recalcular(Ocorrencia o) {
        o.setPrioridade(calcular(o.getUrgencia(), Boolean.TRUE.equals(o.getRiscoImediato()),
                o.getQtdApoios() == null ? 0 : o.getQtdApoios(), o.getCriadoEm(), LocalDateTime.now()));
    }
}
