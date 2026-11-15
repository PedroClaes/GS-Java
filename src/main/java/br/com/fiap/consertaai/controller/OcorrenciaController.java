package br.com.fiap.consertaai.controller;

import br.com.fiap.consertaai.domain.enums.StatusOcorrencia;
import br.com.fiap.consertaai.dto.OcorrenciaDtos.*;
import br.com.fiap.consertaai.dto.PaginaResponse;
import br.com.fiap.consertaai.ia.TriagemService;
import br.com.fiap.consertaai.security.UsuarioLogado;
import br.com.fiap.consertaai.service.OcorrenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@Tag(name = "Ocorrências", description = "Problemas de zeladoria: abertura, acompanhamento e fila do gestor")
@RestController
@RequestMapping("/ocorrencias")
@RequiredArgsConstructor
public class OcorrenciaController {

    private final OcorrenciaService ocorrenciaService;
    private final TriagemService triagemService;

    @Operation(summary = "Lista ocorrências da cidade (público), com filtros e paginação")
    @GetMapping
    public PaginaResponse<OcorrenciaResponse> listar(
            @RequestParam(required = false) StatusOcorrencia status,
            @RequestParam(required = false) String bairro,
            @RequestParam(required = false) Long categoriaId,
            @PageableDefault(size = 10, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return PaginaResponse.de(ocorrenciaService.listar(status, bairro, categoriaId, pageable));
    }

    @Operation(summary = "Ocorrências abertas pelo cidadão logado (CIDADAO)")
    @GetMapping("/minhas")
    public PaginaResponse<OcorrenciaResponse> minhas(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 10, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return PaginaResponse.de(ocorrenciaService.minhas(Long.valueOf(jwt.getSubject()), pageable));
    }

    @Operation(summary = "Fila de atendimento ordenada por prioridade (GESTOR)")
    @GetMapping("/fila")
    public PaginaResponse<OcorrenciaResponse> fila(@PageableDefault(size = 20) Pageable pageable) {
        return PaginaResponse.de(ocorrenciaService.fila(pageable));
    }

    @Operation(summary = "Detalhe de uma ocorrência (público; também usado pela API .NET)")
    @GetMapping("/{id}")
    public OcorrenciaResponse buscar(@PathVariable Long id) {
        return ocorrenciaService.buscar(id);
    }

    @Operation(summary = "Linha do tempo de status (público)")
    @GetMapping("/{id}/historico")
    public List<HistoricoResponse> historico(@PathVariable Long id) {
        return ocorrenciaService.historico(id);
    }

    @Operation(summary = "Abre uma ocorrência (CIDADAO). Volta com status EM_TRIAGEM; a IA classifica em seguida")
    @PostMapping
    public ResponseEntity<OcorrenciaResponse> criar(@AuthenticationPrincipal Jwt jwt,
                                                    @RequestBody @Valid OcorrenciaRequest request) {
        OcorrenciaResponse criada = ocorrenciaService.criar(request, Long.valueOf(jwt.getSubject()));
        var uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(uri).body(criada);
    }

    @Operation(summary = "Edita a ocorrência (somente o autor, enquanto em triagem ou aberta)")
    @PutMapping("/{id}")
    public OcorrenciaResponse atualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                        @RequestBody @Valid OcorrenciaRequest request) {
        return ocorrenciaService.atualizar(id, request, UsuarioLogado.de(jwt));
    }

    @Operation(summary = "Exclui (autor enquanto não atendida, ou GESTOR)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        ocorrenciaService.excluir(id, UsuarioLogado.de(jwt));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Muda o status, registrando no histórico (GESTOR)")
    @PatchMapping("/{id}/status")
    public OcorrenciaResponse alterarStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                            @RequestBody @Valid StatusRequest request) {
        return ocorrenciaService.alterarStatus(id, request, Long.valueOf(jwt.getSubject()));
    }

    @Operation(summary = "Refaz a triagem com IA na hora, sem fila (GESTOR)")
    @PostMapping("/{id}/triagem")
    public OcorrenciaResponse refazerTriagem(@PathVariable Long id) {
        ocorrenciaService.buscar(id); // 404 se nao existir
        triagemService.triar(id);
        return ocorrenciaService.buscar(id);
    }
}
