package br.com.fiap.consertaai.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger com o botao Authorize: cole o token do /auth/login (sem escrever "Bearer"). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI consertaAiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Conserta Aí — API Java")
                        .version("1.0.0")
                        .description("""
                                Ocorrências de zeladoria urbana com triagem por IA generativa (Spring AI),
                                autenticação JWT compartilhada com a API .NET e eventos no RabbitMQ.
                                Usuários de teste: maria@consertaai.com (CIDADAO) e gestor@consertaai.com
                                (GESTOR), senha senha123."""))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
