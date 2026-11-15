package br.com.fiap.consertaai.dto;

import br.com.fiap.consertaai.domain.entity.Usuario;
import br.com.fiap.consertaai.domain.enums.Papel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** DTOs de autenticacao, agrupados por serem pequenos e sempre usados juntos. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") String email,
            @NotBlank(message = "Informe a senha") String senha) {}

    /** Cadastro aberto apenas para cidadaos. Gestores sao criados pela prefeitura. */
    public record RegistroRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 100, message = "Nome com no máximo 100 caracteres") String nome,
            @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") @Size(max = 150) String email,
            @NotBlank(message = "Informe a senha") @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres") String senha,
            @Size(max = 80, message = "Bairro com no máximo 80 caracteres") String bairro) {}

    public record UsuarioResponse(Long id, String nome, String email, Papel papel, String bairro) {
        public static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getPapel(), u.getBairro());
        }
    }

    public record TokenResponse(String token, String tipo, Instant expiraEm, UsuarioResponse usuario) {}
}
