package br.com.fiap.consertaai.ia;

import br.com.fiap.consertaai.domain.enums.OrigemTriagem;
import br.com.fiap.consertaai.domain.enums.Urgencia;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TriagemRegrasTest {

    private final TriagemRegras regras = new TriagemRegras();

    private TriagemEntrada entrada(String titulo, String descricao, List<TriagemEntrada.Candidata> candidatas) {
        return new TriagemEntrada(10L, titulo, descricao, "Centro", null, List.of(), candidatas);
    }

    @Test
    void classificaPorPalavraChaveSemAcento() {
        var r = regras.classificar(entrada("Poste apagado", "A lâmpada do poste não acende", List.of()));
        assertThat(r.categoria()).isEqualTo("Iluminação pública");
        assertThat(r.origem()).isEqualTo(OrigemTriagem.REGRAS);
    }

    @Test
    void fiacaoExpostaECriticaComRisco() {
        var r = regras.classificar(entrada("Fio solto na calçada", "Tem fiação exposta perto do ponto de ônibus", List.of()));
        assertThat(r.urgencia()).isEqualTo(Urgencia.CRITICA);
        assertThat(r.riscoImediato()).isTrue();
    }

    @Test
    void semPalavraConhecidaCaiEmOutros() {
        var r = regras.classificar(entrada("Barulho de obra", "Obra fazendo barulho de madrugada", List.of()));
        assertThat(r.categoria()).isEqualTo("Outros");
        assertThat(r.urgencia()).isEqualTo(Urgencia.MEDIA);
    }

    @Test
    void apontaDuplicadaComTextoParecidoNaMesmaCategoria() {
        var candidata = new TriagemEntrada.Candidata(1L, "Buraco grande na Rua XV de Novembro",
                "Buraco enorme perto da faixa de pedestres", "Buraco na via");
        var r = regras.classificar(entrada("Buraco enorme na Rua XV de Novembro",
                "Buraco grande perto da faixa de pedestres", List.of(candidata)));
        assertThat(r.possivelDuplicadaDeId()).isEqualTo(1L);
    }

    @Test
    void naoApontaDuplicadaDeOutraCategoria() {
        var candidata = new TriagemEntrada.Candidata(2L, "Poste apagado na Rua XV",
                "Poste apagado na Rua XV de Novembro", "Iluminação pública");
        var r = regras.classificar(entrada("Buraco na Rua XV de Novembro", "Buraco na Rua XV de Novembro", List.of(candidata)));
        assertThat(r.possivelDuplicadaDeId()).isNull();
    }
}
