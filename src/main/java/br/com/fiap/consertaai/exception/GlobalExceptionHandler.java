package br.com.fiap.consertaai.exception;

import br.com.fiap.consertaai.dto.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;

/**
 * Tratamento global: toda excecao vira um {@link ErroResponse} com o mesmo formato,
 * e nenhum stack trace chega ao cliente.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex, HttpServletRequest req) {
        var campos = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new ErroResponse.CampoInvalido(f.getField(), f.getDefaultMessage()))
                .toList();
        var corpo = new ErroResponse(Instant.now(), 400, "Dados inválidos",
                "Um ou mais campos não passaram na validação.", req.getRequestURI(), campos);
        return ResponseEntity.badRequest().body(corpo);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido",
                "O JSON enviado está malformado ou tem um valor inválido.", req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> parametroInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetro inválido",
                "O parâmetro '" + ex.getName() + "' tem um valor inválido.", req);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErroResponse> ordenacaoInvalida(PropertyReferenceException ex, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetro inválido",
                "Não é possível ordenar pelo campo '" + ex.getPropertyName() + "'.", req);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest req) {
        return resposta(HttpStatus.NOT_FOUND, "Não encontrado", ex.getMessage(), req);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException ex, HttpServletRequest req) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio", ex.getMessage(), req);
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErroResponse> conflito(ConflitoException ex, HttpServletRequest req) {
        return resposta(HttpStatus.CONFLICT, "Conflito", ex.getMessage(), req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> integridade(DataIntegrityViolationException ex, HttpServletRequest req) {
        return resposta(HttpStatus.CONFLICT, "Conflito",
                "A operação viola uma restrição do banco de dados (registro duplicado ou em uso).", req);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErroResponse> credenciais(BadCredentialsException ex, HttpServletRequest req) {
        return resposta(HttpStatus.UNAUTHORIZED, "Não autenticado", "E-mail ou senha inválidos.", req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> acessoNegado(AccessDeniedException ex, HttpServletRequest req) {
        return resposta(HttpStatus.FORBIDDEN, "Acesso negado", ex.getMessage(), req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> metodo(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, "Método não permitido", ex.getMessage(), req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> rotaInexistente(NoResourceFoundException ex, HttpServletRequest req) {
        return resposta(HttpStatus.NOT_FOUND, "Não encontrado", "Rota inexistente.", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> inesperado(Exception ex, HttpServletRequest req) {
        log.error("Erro inesperado em {} {}", req.getMethod(), req.getRequestURI(), ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente.", req);
    }

    private ResponseEntity<ErroResponse> resposta(HttpStatus status, String erro, String mensagem, HttpServletRequest req) {
        return ResponseEntity.status(status).body(ErroResponse.de(status.value(), erro, mensagem, req.getRequestURI()));
    }
}
