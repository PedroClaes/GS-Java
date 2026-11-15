package br.com.fiap.consertaai.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categorias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String nome;

    @Column(length = 255)
    private String descricao;

    /** Orgao que atende essa categoria. A triagem usa como sugestao de encaminhamento. */
    @Column(name = "orgao_responsavel", nullable = false, length = 120)
    private String orgaoResponsavel;

    /** Prazo de atendimento, em dias, que a prefeitura se compromete a cumprir. */
    @Column(name = "prazo_sla_dias", nullable = false)
    private Integer prazoSlaDias;

    @Builder.Default
    @Column(nullable = false)
    private Boolean ativa = Boolean.TRUE;
}
