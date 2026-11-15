package br.com.fiap.consertaai.controller;

import br.com.fiap.consertaai.dto.CategoriaDtos.CategoriaRequest;
import br.com.fiap.consertaai.dto.CategoriaDtos.CategoriaResponse;
import br.com.fiap.consertaai.dto.PaginaResponse;
import br.com.fiap.consertaai.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@Tag(name = "Categorias", description = "Tipos de problema e órgão responsável (escrita: GESTOR)")
@RestController
@RequestMapping("/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;

    @Operation(summary = "Categorias ativas (público, usado no formulário do app)")
    @GetMapping
    public List<CategoriaResponse> listarAtivas() {
        return categoriaService.listarAtivas();
    }

    @Operation(summary = "Todas as categorias, paginadas (GESTOR)")
    @GetMapping("/todas")
    public PaginaResponse<CategoriaResponse> listarTodas(
            @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return PaginaResponse.de(categoriaService.listarTodas(pageable));
    }

    @Operation(summary = "Detalhe de uma categoria")
    @GetMapping("/{id}")
    public CategoriaResponse buscar(@PathVariable Long id) {
        return categoriaService.buscar(id);
    }

    @Operation(summary = "Cria categoria (GESTOR)")
    @PostMapping
    public ResponseEntity<CategoriaResponse> criar(@RequestBody @Valid CategoriaRequest request) {
        CategoriaResponse criada = categoriaService.criar(request);
        var uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(uri).body(criada);
    }

    @Operation(summary = "Atualiza categoria (GESTOR)")
    @PutMapping("/{id}")
    public CategoriaResponse atualizar(@PathVariable Long id, @RequestBody @Valid CategoriaRequest request) {
        return categoriaService.atualizar(id, request);
    }

    @Operation(summary = "Exclui categoria sem ocorrências (GESTOR)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        categoriaService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
