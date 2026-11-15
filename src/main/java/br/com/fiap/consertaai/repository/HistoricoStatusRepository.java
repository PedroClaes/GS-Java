package br.com.fiap.consertaai.repository;

import br.com.fiap.consertaai.domain.entity.HistoricoStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoStatusRepository extends JpaRepository<HistoricoStatus, Long> {

    @EntityGraph(attributePaths = {"responsavel"})
    List<HistoricoStatus> findByOcorrenciaIdOrderByDataAscIdAsc(Long ocorrenciaId);
}
