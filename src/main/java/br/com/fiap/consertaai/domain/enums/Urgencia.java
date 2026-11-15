package br.com.fiap.consertaai.domain.enums;

/**
 * Urgencia definida na triagem. O peso entra no calculo da prioridade da fila do gestor.
 */
public enum Urgencia {
    BAIXA(15),
    MEDIA(30),
    ALTA(50),
    CRITICA(65);

    private final int peso;

    Urgencia(int peso) {
        this.peso = peso;
    }

    public int getPeso() {
        return peso;
    }

    /** Converte o texto devolvido pela IA, aceitando variacoes como "media", "Média" ou "alto". */
    public static Urgencia deTexto(String texto) {
        if (texto == null) {
            return MEDIA;
        }
        String normalizado = java.text.Normalizer.normalize(texto.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase();
        if (normalizado.startsWith("CRIT")) return CRITICA;
        if (normalizado.startsWith("ALT")) return ALTA;
        if (normalizado.startsWith("BAIX")) return BAIXA;
        return MEDIA;
    }
}
