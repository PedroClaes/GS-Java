package br.com.fiap.consertaai.domain.entity;

import br.com.fiap.consertaai.domain.enums.StatusOcorrencia;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Linha do tempo do status de uma ocorrencia: o cidadao acompanha cada mudanca. */
@Entity
@Table(name = "historico_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricoStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ocorrencia_id", nullable = false)
    private Ocorrencia ocorrencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 20)
    private StatusOcorrencia statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 20)
    private StatusOcorrencia statusNovo;

    @Column(length = 500)
    private String observacao;

    /** Nulo quando a mudanca foi feita pelo sistema (fim da triagem). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id")
    private Usuario responsavel;

    @Column(nullable = false)
    private LocalDateTime data;

    @PrePersist
    void aoCriar() {
        if (data == null) data = LocalDateTime.now();
    }
}
