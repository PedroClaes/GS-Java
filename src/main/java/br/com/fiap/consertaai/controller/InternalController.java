package br.com.fiap.consertaai.controller;

import br.com.fiap.consertaai.dto.OcorrenciaDtos.ApoiosRequest;
import br.com.fiap.consertaai.service.OcorrenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Plano B da integracao com a API .NET: quando ela estiver configurada para HTTP em vez
 * de RabbitMQ, chama este endpoint com o total de apoios (repassando o token do cidadao).
 */
@Tag(name = "Integração", description = "Endpoints chamados pela API .NET")
@RestController
@RequestMapping("/internal/ocorrencias")
@RequiredArgsConstructor
public class InternalController {

    private final OcorrenciaService ocorrenciaService;

    @Operation(summary = "Atualiza o total de apoios (plano B do evento apoio.*)")
    @PatchMapping("/{id}/apoios")
    public ResponseEntity<Void> atualizarApoios(@PathVariable Long id, @RequestBody @Valid ApoiosRequest request) {
        boolean aplicado = ocorrenciaService.atualizarApoios(id, request.totalApoios(), null);
        return aplicado ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
