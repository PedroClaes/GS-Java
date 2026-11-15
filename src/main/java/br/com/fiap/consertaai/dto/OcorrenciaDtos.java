package br.com.fiap.consertaai.dto;

import br.com.fiap.consertaai.domain.entity.HistoricoStatus;
import br.com.fiap.consertaai.domain.entity.Ocorrencia;
import br.com.fiap.consertaai.domain.enums.OrigemTriagem;
import br.com.fiap.consertaai.domain.enums.StatusOcorrencia;
import br.com.fiap.consertaai.domain.enums.Urgencia;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public final class OcorrenciaDtos {

    private OcorrenciaDtos() {
    }

    /** Usado para criar (POST) e editar (PUT). A categoria e opcional: se vier vazia, a IA escolhe. */
    public record OcorrenciaRequest(
            @NotBlank(message = "Informe um título") @Size(min = 5, max = 120, message = "O título deve ter entre 5 e 120 caracteres")
            String titulo,
            @NotBlank(message = "Descreva o problema") @Size(min = 10, max = 1000, message = "A descrição deve ter entre 10 e 1000 caracteres")
            String descricao,
            @NotBlank(message = "Informe o endereço") @Size(max = 200) String endereco,
            @NotBlank(message = "Informe o bairro") @Size(max = 80) String bairro,
            @DecimalMin(value = "-90.0", message = "Latitude inválida") @DecimalMax(value = "90.0", message = "Latitude inválida")
            BigDecimal latitude,
            @DecimalMin(value = "-180.0", message = "Longitude inválida") @DecimalMax(value = "180.0", message = "Longitude inválida")
            BigDecimal longitude,
            Long categoriaId) {}

    public record StatusRequest(
            @NotNull(message = "Informe o novo status") StatusOcorrencia status,
            @Size(max = 500, message = "Observação com no máximo 500 caracteres") String observacao) {}

    /** Corpo do PATCH /internal/ocorrencias/{id}/apoios enviado pela API .NET (plano B do RabbitMQ). */
    public record ApoiosRequest(
            @NotNull(message = "Informe o total de apoios") @Min(value = 0, message = "O total não pode ser negativo")
            Integer totalApoios,
            String tipo,
            UUID eventoId) {}

    public record CategoriaResumo(Long id, String nome) {}

    public record AutorResumo(Long id, String nome) {}

    public record OcorrenciaResponse(
            Long id,
            String titulo,
            String descricao,
            String endereco,
            String bairro,
            BigDecimal latitude,
            BigDecimal longitude,
            StatusOcorrencia status,
            CategoriaResumo categoria,
            Urgencia urgencia,
            Boolean riscoImediato,
            String resumoIa,
            String orgaoSugerido,
            OrigemTriagem origemTriagem,
            Long possivelDuplicadaDeId,
            Integer qtdApoios,
            Integer prioridade,
            LocalDate prazoAtendimento,
            AutorResumo autor,
            LocalDateTime criadoEm,
            LocalDateTime atualizadoEm,
            LocalDateTime triadaEm) {

        public static OcorrenciaResponse de(Ocorrencia o) {
            var categoria = o.getCategoria();
            LocalDate prazo = categoria == null ? null
                    : o.getCriadoEm().toLocalDate().plusDays(categoria.getPrazoSlaDias());
            return new OcorrenciaResponse(
                    o.getId(), o.getTitulo(), o.getDescricao(), o.getEndereco(), o.getBairro(),
                    o.getLatitude(), o.getLongitude(), o.getStatus(),
                    categoria == null ? null : new CategoriaResumo(categoria.getId(), categoria.getNome()),
                    o.getUrgencia(), o.getRiscoImediato(), o.getResumoIa(), o.getOrgaoSugerido(), o.getOrigemTriagem(),
                    o.getPossivelDuplicadaDe() == null ? null : o.getPossivelDuplicadaDe().getId(),
                    o.getQtdApoios(), o.getPrioridade(), prazo,
                    new AutorResumo(o.getAutor().getId(), o.getAutor().getNome()),
                    o.getCriadoEm(), o.getAtualizadoEm(), o.getTriadaEm());
        }
    }

    public record HistoricoResponse(
            StatusOcorrencia statusAnterior,
            StatusOcorrencia statusNovo,
            String observacao,
            String responsavel,
            LocalDateTime data) {

        public static HistoricoResponse de(HistoricoStatus h) {
            return new HistoricoResponse(h.getStatusAnterior(), h.getStatusNovo(), h.getObservacao(),
                    h.getResponsavel() == null ? "Sistema" : h.getResponsavel().getNome(), h.getData());
        }
    }
}
