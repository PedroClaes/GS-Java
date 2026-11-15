package br.com.fiap.consertaai.domain.entity;

import br.com.fiap.consertaai.domain.enums.OrigemTriagem;
import br.com.fiap.consertaai.domain.enums.StatusOcorrencia;
import br.com.fiap.consertaai.domain.enums.Urgencia;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Problema de zeladoria reportado por um cidadao (buraco, poste apagado, lixo...).
 *
 * Os campos de triagem (categoria, urgencia, risco, resumo, duplicada) sao preenchidos
 * de forma assincrona pela IA depois que a ocorrencia e criada. qtdApoios vem da API .NET,
 * pelo RabbitMQ.
 */
@Entity
@Table(name = "ocorrencias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ocorrencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String descricao;

    @Column(nullable = false, length = 200)
    private String endereco;

    @Column(nullable = false, length = 80)
    private String bairro;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusOcorrencia status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Urgencia urgencia;

    @Builder.Default
    @Column(name = "risco_imediato", nullable = false)
    private Boolean riscoImediato = Boolean.FALSE;

    @Column(name = "resumo_ia", length = 500)
    private String resumoIa;

    @Column(name = "orgao_sugerido", length = 120)
    private String orgaoSugerido;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_triagem", length = 10)
    private OrigemTriagem origemTriagem;

    /** Ocorrencia anterior que parece descrever o mesmo problema (sugestao de "apoiar" em vez de duplicar). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "possivel_duplicada_de")
    private Ocorrencia possivelDuplicadaDe;

    @Column(name = "triada_em")
    private LocalDateTime triadaEm;

    @Builder.Default
    @Column(name = "qtd_apoios", nullable = false)
    private Integer qtdApoios = 0;

    /** Data do ultimo evento de apoio aplicado: descarta eventos que chegam fora de ordem. */
    @Column(name = "ultimo_evento_apoio_em")
    private LocalDateTime ultimoEventoApoioEm;

    @Builder.Default
    @Column(nullable = false)
    private Integer prioridade = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    void aoCriar() {
        LocalDateTime agora = LocalDateTime.now();
        if (criadoEm == null) criadoEm = agora;
        atualizadoEm = agora;
    }

    @PreUpdate
    void aoAtualizar() {
        atualizadoEm = LocalDateTime.now();
    }
}
