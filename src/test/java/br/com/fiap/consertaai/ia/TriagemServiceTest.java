package br.com.fiap.consertaai.ia;

import br.com.fiap.consertaai.domain.enums.OrigemTriagem;
import br.com.fiap.consertaai.domain.enums.Urgencia;
import br.com.fiap.consertaai.service.OcorrenciaService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TriagemServiceTest {

    private final OcorrenciaService ocorrenciaService = mock(OcorrenciaService.class);
    private final TriagemIaClient iaClient = mock(TriagemIaClient.class);
    private final TriagemRegras regras = new TriagemRegras();

    private final TriagemEntrada entrada = new TriagemEntrada(5L, "Poste apagado", "Poste apagado na esquina",
            "Centro", null, List.of(), List.of());

    @Test
    void usaOResultadoDaIaQuandoElaResponde() {
        var service = new TriagemService(ocorrenciaService, iaClient, regras, true, "chave-valida");
        var daIa = new ResultadoTriagem("Iluminação pública", Urgencia.MEDIA, false, "Iluminação", "Poste apagado",
                null, OrigemTriagem.IA);
        when(ocorrenciaService.dadosParaTriagem(5L)).thenReturn(Optional.of(entrada));
        when(iaClient.classificar(entrada)).thenReturn(daIa);

        service.triar(5L);

        verify(ocorrenciaService).aplicarTriagem(5L, daIa);
    }

    @Test
    void caiNasRegrasQuandoAIaFalha() {
        var service = new TriagemService(ocorrenciaService, iaClient, regras, true, "chave-valida");
        when(ocorrenciaService.dadosParaTriagem(5L)).thenReturn(Optional.of(entrada));
        when(iaClient.classificar(any())).thenThrow(new RuntimeException("401 Unauthorized"));

        service.triar(5L);

        verify(ocorrenciaService).aplicarTriagem(eq(5L), argThat(r -> r.origem() == OrigemTriagem.REGRAS));
    }

    @Test
    void semChaveNemChamaAIa() {
        var service = new TriagemService(ocorrenciaService, iaClient, regras, true, "sem-chave");
        when(ocorrenciaService.dadosParaTriagem(5L)).thenReturn(Optional.of(entrada));

        service.triar(5L);

        verifyNoInteractions(iaClient);
        verify(ocorrenciaService).aplicarTriagem(eq(5L), argThat(r -> r.origem() == OrigemTriagem.REGRAS));
    }

    @Test
    void ocorrenciaExcluidaNaoETriada() {
        var service = new TriagemService(ocorrenciaService, iaClient, regras, true, "chave-valida");
        when(ocorrenciaService.dadosParaTriagem(5L)).thenReturn(Optional.empty());

        service.triar(5L);

        verify(ocorrenciaService, never()).aplicarTriagem(any(), any());
    }
}
