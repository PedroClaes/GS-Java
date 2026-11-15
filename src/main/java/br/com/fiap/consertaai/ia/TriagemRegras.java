package br.com.fiap.consertaai.ia;

import br.com.fiap.consertaai.domain.enums.OrigemTriagem;
import br.com.fiap.consertaai.domain.enums.Urgencia;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.*;

/**
 * Plano B da triagem, usado quando o modelo de IA esta indisponivel, sem chave ou
 * devolve algo invalido. Palavras-chave simples: menos precisas, mas a ocorrencia
 * nunca fica parada em EM_TRIAGEM por falha externa.
 */
@Component
public class TriagemRegras {

    private static final Map<String, List<String>> PALAVRAS_POR_CATEGORIA = new LinkedHashMap<>();

    static {
        PALAVRAS_POR_CATEGORIA.put("Iluminação pública", List.of("poste", "lampada", "luz", "iluminacao", "apagad", "escuro"));
        PALAVRAS_POR_CATEGORIA.put("Buraco na via", List.of("buraco", "cratera", "asfalto", "afundamento", "pavimento"));
        PALAVRAS_POR_CATEGORIA.put("Lixo e entulho", List.of("lixo", "entulho", "descarte", "sujeira", "rato", "lixeira"));
        PALAVRAS_POR_CATEGORIA.put("Calçada e acessibilidade", List.of("calcada", "rampa", "cadeirante", "piso", "passeio"));
        PALAVRAS_POR_CATEGORIA.put("Sinalização de trânsito", List.of("semaforo", "placa", "sinalizacao", "faixa", "transito"));
        PALAVRAS_POR_CATEGORIA.put("Árvores e áreas verdes", List.of("arvore", "galho", "poda", "mato", "praca"));
        PALAVRAS_POR_CATEGORIA.put("Alagamento e esgoto", List.of("bueiro", "esgoto", "alag", "enchente", "boca de lobo", "vazamento"));
    }

    private static final List<String> CRITICO = List.of("fio solto", "fiacao exposta", "choque", "desab", "prestes a cair", "correnteza");
    private static final List<String> ALTO = List.of("acidente", "perigo", "risco", "crianca", "escola", "hospital", "idoso", "contramao", "fio");

    public ResultadoTriagem classificar(TriagemEntrada entrada) {
        String texto = normalizar(entrada.titulo() + " " + entrada.descricao());

        String categoria = entrada.categoriaInformada() != null
                ? entrada.categoriaInformada()
                : PALAVRAS_POR_CATEGORIA.entrySet().stream()
                        .filter(e -> e.getValue().stream().anyMatch(texto::contains))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse("Outros");

        boolean critico = CRITICO.stream().anyMatch(texto::contains);
        boolean alto = ALTO.stream().anyMatch(texto::contains);
        Urgencia urgencia = critico ? Urgencia.CRITICA : alto ? Urgencia.ALTA : Urgencia.MEDIA;

        return new ResultadoTriagem(categoria, urgencia, critico, null, null,
                duplicadaMaisProvavel(texto, categoria, entrada.candidatas()), OrigemTriagem.REGRAS);
    }

    /** Mesma categoria e muitas palavras em comum (Jaccard >= 0,35) com uma ocorrencia aberta do bairro. */
    Long duplicadaMaisProvavel(String texto, String categoria, List<TriagemEntrada.Candidata> candidatas) {
        Set<String> palavras = palavras(texto);
        Long melhorId = null;
        double melhor = 0.35;
        for (TriagemEntrada.Candidata c : candidatas) {
            if (c.categoria() != null && !c.categoria().equalsIgnoreCase(categoria)) continue;
            double similaridade = jaccard(palavras, palavras(normalizar(c.titulo() + " " + c.descricao())));
            if (similaridade >= melhor) {
                melhor = similaridade;
                melhorId = c.id();
            }
        }
        return melhorId;
    }

    static String normalizar(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private static Set<String> palavras(String texto) {
        Set<String> set = new HashSet<>();
        for (String p : texto.split("[^a-z0-9]+")) {
            if (p.length() > 3) set.add(p); // ignora artigos e preposicoes curtas
        }
        return set;
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        Set<String> intersecao = new HashSet<>(a);
        intersecao.retainAll(b);
        Set<String> uniao = new HashSet<>(a);
        uniao.addAll(b);
        return (double) intersecao.size() / uniao.size();
    }
}
