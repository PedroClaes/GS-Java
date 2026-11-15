package br.com.fiap.consertaai.service;

import br.com.fiap.consertaai.domain.entity.Usuario;
import br.com.fiap.consertaai.domain.enums.Papel;
import br.com.fiap.consertaai.dto.AuthDtos.*;
import br.com.fiap.consertaai.exception.ConflitoException;
import br.com.fiap.consertaai.exception.RecursoNaoEncontradoException;
import br.com.fiap.consertaai.repository.UsuarioRepository;
import br.com.fiap.consertaai.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(request.email().trim())
                .filter(u -> passwordEncoder.matches(request.senha(), u.getSenhaHash()))
                // Mesma mensagem para e-mail inexistente e senha errada: nao revela quais e-mails existem
                .orElseThrow(() -> new BadCredentialsException("E-mail ou senha inválidos."));
        return gerarToken(usuario);
    }

    /** Cadastro publico cria sempre um CIDADAO: ninguem se promove a gestor pela API. */
    @Transactional
    public TokenResponse registrar(RegistroRequest request) {
        String email = request.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflitoException("Já existe uma conta com este e-mail.");
        }
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nome(request.nome().trim())
                .email(email)
                .senhaHash(passwordEncoder.encode(request.senha()))
                .papel(Papel.CIDADAO)
                .bairro(request.bairro() == null ? null : request.bairro().trim())
                .build());
        return gerarToken(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscar(Long id) {
        return usuarioRepository.findById(id)
                .map(UsuarioResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
    }

    private TokenResponse gerarToken(Usuario usuario) {
        var token = tokenService.emitir(usuario);
        return new TokenResponse(token.valor(), "Bearer", token.expiraEm(), UsuarioResponse.de(usuario));
    }
}
