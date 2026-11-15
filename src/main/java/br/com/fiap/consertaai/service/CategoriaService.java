package br.com.fiap.consertaai.service;

import br.com.fiap.consertaai.domain.entity.Categoria;
import br.com.fiap.consertaai.dto.CategoriaDtos.CategoriaRequest;
import br.com.fiap.consertaai.dto.CategoriaDtos.CategoriaResponse;
import br.com.fiap.consertaai.exception.ConflitoException;
import br.com.fiap.consertaai.exception.RecursoNaoEncontradoException;
import br.com.fiap.consertaai.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarAtivas() {
        return categoriaRepository.findByAtivaTrueOrderByNomeAsc().stream().map(CategoriaResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<CategoriaResponse> listarTodas(Pageable pageable) {
        return categoriaRepository.findAll(pageable).map(CategoriaResponse::de);
    }

    @Transactional(readOnly = true)
    public CategoriaResponse buscar(Long id) {
        return CategoriaResponse.de(obter(id));
    }

    @Transactional
    public CategoriaResponse criar(CategoriaRequest request) {
        if (categoriaRepository.existsByNomeIgnoreCase(request.nome().trim())) {
            throw new ConflitoException("Já existe uma categoria com este nome.");
        }
        Categoria categoria = new Categoria();
        aplicar(categoria, request);
        return CategoriaResponse.de(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResponse atualizar(Long id, CategoriaRequest request) {
        Categoria categoria = obter(id);
        if (categoriaRepository.existsByNomeIgnoreCaseAndIdNot(request.nome().trim(), id)) {
            throw new ConflitoException("Já existe uma categoria com este nome.");
        }
        aplicar(categoria, request);
        return CategoriaResponse.de(categoria);
    }

    /** Categoria em uso por ocorrencias nao e apagada: a FK impede, e o handler responde 409. */
    @Transactional
    public void excluir(Long id) {
        categoriaRepository.delete(obter(id));
        categoriaRepository.flush();
    }

    private Categoria obter(Long id) {
        return categoriaRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Categoria", id));
    }

    private void aplicar(Categoria c, CategoriaRequest r) {
        c.setNome(r.nome().trim());
        c.setDescricao(r.descricao());
        c.setOrgaoResponsavel(r.orgaoResponsavel().trim());
        c.setPrazoSlaDias(r.prazoSlaDias());
        c.setAtiva(r.ativa() == null ? Boolean.TRUE : r.ativa());
    }
}
