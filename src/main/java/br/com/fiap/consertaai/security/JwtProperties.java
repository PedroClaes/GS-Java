package br.com.fiap.consertaai.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracao do JWT, lida de consertaai.jwt.* (variaveis JWT_SECRET, JWT_ISSUER, JWT_AUDIENCE).
 * Os tres valores sao os mesmos da API .NET: um token emitido aqui e aceito la.
 */
@ConfigurationProperties(prefix = "consertaai.jwt")
public record JwtProperties(String secret, String issuer, String audience, long expiracaoMinutos) {

    public JwtProperties {
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            // HS256 exige chave de pelo menos 256 bits; falhar cedo evita tokens fracos em producao.
            throw new IllegalStateException("JWT_SECRET precisa ter pelo menos 32 caracteres.");
        }
        if (expiracaoMinutos <= 0) {
            expiracaoMinutos = 120;
        }
    }
}
