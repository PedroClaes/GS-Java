package br.com.fiap.consertaai.repository;

import br.com.fiap.consertaai.domain.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByAtivaTrueOrderByNomeAsc();

    Optional<Categoria> findByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
