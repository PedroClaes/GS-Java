package br.com.fiap.consertaai.config;

import br.com.fiap.consertaai.security.JsonSecurityHandlers;
import br.com.fiap.consertaai.security.JwtProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Seguranca stateless com JWT.
 *
 * Fluxo: POST /auth/login confere a senha (BCrypt) e devolve um JWT assinado com HS256.
 * Em cada requisicao, o BearerTokenAuthenticationFilter valida assinatura, emissor,
 * audiencia e expiracao; o claim "roles" vira ROLE_CIDADAO / ROLE_GESTOR.
 * Nao ha sessao: o servidor nao guarda estado entre requisicoes.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String CIDADAO = "CIDADAO";
    private static final String GESTOR = "GESTOR";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JsonSecurityHandlers handlers) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // API stateless com token no cabecalho: sem cookie, sem CSRF
                .cors(cors -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Publicos
                        .requestMatchers("/auth/login", "/auth/registro").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.GET, "/categorias/**").permitAll()

                        // Especificos antes dos genericos: a primeira regra que casa e a que vale
                        .requestMatchers(HttpMethod.GET, "/ocorrencias/minhas").hasRole(CIDADAO)
                        .requestMatchers(HttpMethod.GET, "/ocorrencias/fila").hasRole(GESTOR)
                        // Transparencia: qualquer pessoa ve as ocorrencias da cidade e o historico.
                        // A API .NET tambem consulta GET /ocorrencias/{id} para validar o status.
                        .requestMatchers(HttpMethod.GET, "/ocorrencias", "/ocorrencias/*", "/ocorrencias/*/historico").permitAll()

                        .requestMatchers(HttpMethod.POST, "/ocorrencias").hasRole(CIDADAO)
                        .requestMatchers(HttpMethod.PATCH, "/ocorrencias/*/status").hasRole(GESTOR)
                        .requestMatchers(HttpMethod.POST, "/ocorrencias/*/triagem").hasRole(GESTOR)
                        // PUT e DELETE: autenticado; o service confere se e o autor (ou gestor)
                        .requestMatchers("/ocorrencias/**").authenticated()

                        .requestMatchers("/categorias/**").hasRole(GESTOR)
                        .requestMatchers("/usuarios/**").hasRole(GESTOR)
                        // Chamado pela API .NET repassando o token do cidadao
                        .requestMatchers("/internal/**").authenticated()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(handlers)
                        .accessDeniedHandler(handlers))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(handlers)
                        .accessDeniedHandler(handlers));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecretKey jwtSecretKey(JwtProperties props) {
        return new SecretKeySpec(props.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey, JwtProperties props) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        // Alem de assinatura e expiracao: emissor e audiencia precisam ser os do Conserta Ai
        OAuth2TokenValidator<Jwt> audiencia = new JwtClaimValidator<List<String>>(
                JwtClaimNames.AUD, aud -> aud != null && aud.contains(props.audience()));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(props.issuer()), audiencia));
        return decoder;
    }

    /** Converte o claim "roles" (["CIDADAO"]) em autoridades ROLE_CIDADAO, usadas por hasRole(). */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

    /** O app Mobile (Expo web inclusive) chama de outra origem. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Location"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
