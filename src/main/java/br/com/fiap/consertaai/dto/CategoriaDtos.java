package br.com.fiap.consertaai.dto;

import br.com.fiap.consertaai.domain.entity.Categoria;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class CategoriaDtos {

    private CategoriaDtos() {
    }

    public record CategoriaRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 60) String nome,
            @Size(max = 255) String descricao,
            @NotBlank(message = "Informe o órgão responsável") @Size(max = 120) String orgaoResponsavel,
            @NotNull(message = "Informe o prazo em dias") @Min(value = 1, message = "Prazo mínimo de 1 dia")
            @Max(value = 365, message = "Prazo máximo de 365 dias") Integer prazoSlaDias,
            Boolean ativa) {}

    public record CategoriaResponse(Long id, String nome, String descricao, String orgaoResponsavel,
                                    Integer prazoSlaDias, Boolean ativa) {
        public static CategoriaResponse de(Categoria c) {
            return new CategoriaResponse(c.getId(), c.getNome(), c.getDescricao(), c.getOrgaoResponsavel(),
                    c.getPrazoSlaDias(), c.getAtiva());
        }
    }
}
