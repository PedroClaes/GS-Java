package br.com.fiap.consertaai.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Envelope de paginacao estavel (o JSON padrao de Page muda entre versoes do Spring). */
public record PaginaResponse<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas,
        boolean ultima) {

    public static <T> PaginaResponse<T> de(Page<T> page) {
        return new PaginaResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }
}
