package br.com.fiap.consertaai.ia;

import br.com.fiap.consertaai.service.OcorrenciaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Orquestra a triagem em tres passos, sem segurar transacao durante a chamada a IA:
 *   1. le os dados (transacao curta de leitura);
 *   2. classifica com a IA, ou com as regras se a IA falhar;
 *   3. grava o resultado (transacao curta de escrita).
 */
@Slf4j
@Service
public class TriagemService {

    private final OcorrenciaService ocorrenciaService;
    private final TriagemIaClient iaClient;
    private final TriagemRegras regras;
    private final boolean iaHabilitada;

    public TriagemService(OcorrenciaService ocorrenciaService, TriagemIaClient iaClient, TriagemRegras regras,
                          @Value("${consertaai.ia.habilitada:true}") boolean iaHabilitada,
                          @Value("${spring.ai.openai.api-key:}") String apiKey) {
        this.ocorrenciaService = ocorrenciaService;
        this.iaClient = iaClient;
        this.regras = regras;
        this.iaHabilitada = iaHabilitada && apiKey != null && !apiKey.isBlank() && !"sem-chave".equals(apiKey);
        if (!this.iaHabilitada) {
            log.warn("IA desabilitada ou sem AI_API_KEY: a triagem usara as regras de fallback");
        }
    }

    public void triar(Long ocorrenciaId) {
        TriagemEntrada entrada = ocorrenciaService.dadosParaTriagem(ocorrenciaId).orElse(null);
        if (entrada == null) {
            log.info("Ocorrencia {} nao existe mais; triagem ignorada", ocorrenciaId);
            return;
        }
        ocorrenciaService.aplicarTriagem(ocorrenciaId, classificar(entrada));
    }

    ResultadoTriagem classificar(TriagemEntrada entrada) {
        if (iaHabilitada) {
            try {
                return iaClient.classificar(entrada);
            } catch (Exception ex) {
                log.warn("IA indisponivel para a ocorrencia {} ({}); usando regras",
                        entrada.ocorrenciaId(), ex.getMessage());
            }
        }
        return regras.classificar(entrada);
    }

    public boolean iaHabilitada() {
        return iaHabilitada;
    }
}
