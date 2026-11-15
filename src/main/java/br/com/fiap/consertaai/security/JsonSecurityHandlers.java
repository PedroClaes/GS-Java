package br.com.fiap.consertaai.security;

import br.com.fiap.consertaai.dto.ErroResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 401 e 403 gerados pelo filtro de seguranca (antes de chegar ao controller) no mesmo
 * formato de erro do @RestControllerAdvice.
 */
@Component
@RequiredArgsConstructor
public class JsonSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer");
        escrever(response, request, 401, "Não autenticado",
                "Envie um token válido no cabeçalho Authorization: Bearer <token>.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       org.springframework.security.access.AccessDeniedException ex) throws IOException {
        escrever(response, request, 403, "Acesso negado", "Seu perfil não tem permissão para esta operação.");
    }

    private void escrever(HttpServletResponse response, HttpServletRequest request,
                          int status, String erro, String mensagem) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                ErroResponse.de(status, erro, mensagem, request.getRequestURI()));
    }
}
