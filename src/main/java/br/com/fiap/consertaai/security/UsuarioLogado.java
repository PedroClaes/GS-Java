package br.com.fiap.consertaai.security;

import br.com.fiap.consertaai.domain.enums.Papel;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Quem esta chamando a API, extraido do JWT ja validado.
 * O id vem do claim "sub" e o papel do claim "roles": nunca do corpo ou da URL.
 */
public record UsuarioLogado(Long id, Papel papel) {

    public static UsuarioLogado de(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        Papel papel = roles != null && roles.contains(Papel.GESTOR.name()) ? Papel.GESTOR : Papel.CIDADAO;
        return new UsuarioLogado(Long.valueOf(jwt.getSubject()), papel);
    }

    public boolean gestor() {
        return papel == Papel.GESTOR;
    }
}
