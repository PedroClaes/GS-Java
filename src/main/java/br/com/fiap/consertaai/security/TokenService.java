package br.com.fiap.consertaai.security;

import br.com.fiap.consertaai.domain.entity.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Emite o JWT de acesso (HS256).
 *
 * Contrato combinado com a API .NET, que valida o mesmo token:
 *   iss = consertaai, aud = consertaai-app, sub = id do usuario,
 *   roles = ["CIDADAO"] ou ["GESTOR"], name e email para exibicao.
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties props;

    public record TokenEmitido(String valor, Instant expiraEm) {}

    public TokenEmitido emitir(Usuario usuario) {
        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(props.expiracaoMinutos(), ChronoUnit.MINUTES);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .audience(List.of(props.audience()))
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .claim("roles", List.of(usuario.getPapel().name()))
                .claim("name", usuario.getNome())
                .claim("email", usuario.getEmail())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenEmitido(token, expiraEm);
    }
}
