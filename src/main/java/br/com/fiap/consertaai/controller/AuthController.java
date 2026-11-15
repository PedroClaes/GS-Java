package br.com.fiap.consertaai.controller;

import br.com.fiap.consertaai.dto.AuthDtos.*;
import br.com.fiap.consertaai.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Autenticação", description = "Login, cadastro de cidadão e dados do usuário logado")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Login: devolve o JWT usado nas APIs Java e .NET")
    @PostMapping("/login")
    public TokenResponse login(@RequestBody @Valid LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Cadastro de cidadão (já devolve o token)")
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse registrar(@RequestBody @Valid RegistroRequest request) {
        return authService.registrar(request);
    }

    @Operation(summary = "Dados do usuário dono do token")
    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.buscar(Long.valueOf(jwt.getSubject()));
    }
}
