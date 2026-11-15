package br.com.fiap.consertaai.repository;

import br.com.fiap.consertaai.domain.entity.Ocorrencia;
import br.com.fiap.consertaai.domain.enums.StatusOcorrencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Os @EntityGraph carregam categoria e autor na mesma consulta: com open-in-view
 * desligado, a conversao para DTO nao dispara consultas extras (N+1).
 */
public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, Long>, JpaSpecificationExecutor<Ocorrencia> {

    @Override
    @EntityGraph(attributePaths = {"categoria", "autor"})
    Page<Ocorrencia> findAll(Specification<Ocorrencia> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"categoria", "autor", "possivelDuplicadaDe"})
    @Query("select o from Ocorrencia o where o.id = :id")
    Optional<Ocorrencia> buscarDetalhada(@Param("id") Long id);

    @EntityGraph(attributePaths = {"categoria", "autor"})
    Page<Ocorrencia> findByAutorId(Long autorId, Pageable pageable);

    /** Fila do gestor: so o que ainda esta em aberto, da maior para a menor prioridade. */
    @EntityGraph(attributePaths = {"categoria", "autor"})
    Page<Ocorrencia> findByStatusInOrderByPrioridadeDescCriadoEmAsc(Collection<StatusOcorrencia> status, Pageable pageable);

    /** Candidatas a duplicada que a triagem compara com a nova ocorrencia. */
    @EntityGraph(attributePaths = {"categoria"})
    List<Ocorrencia> findTop15ByBairroIgnoreCaseAndStatusInAndIdNotOrderByCriadoEmDesc(
            String bairro, Collection<StatusOcorrencia> status, Long id);
}
