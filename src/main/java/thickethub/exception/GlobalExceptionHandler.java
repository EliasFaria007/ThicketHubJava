package thickethub.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidacao(MethodArgumentNotValidException ex) {
        List<String> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        log.warn("[VALIDAÇÃO] Requisição com dados inválidos — campos com erro: {}", erros);
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Requisição inválida");
        pd.setType(URI.create("https://thickethub.defensoria.mg.gov.br/errors/bad-request"));
        pd.setProperty("erros", erros);
        return pd;
    }

    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ProblemDetail handleRequisicaoMalFormatada(Exception ex) {
        log.warn("[REQUISIÇÃO INVÁLIDA] Formato de dados incorreto: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Corpo da requisição inválido");
        pd.setDetail("Formato de dados inválido ou campo com valor incorreto.");
        return pd;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNaoEncontrado(ResourceNotFoundException ex) {
        log.warn("[NÃO ENCONTRADO] Recurso não existe no banco: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Recurso não encontrado");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleRegraNegocio(BusinessException ex) {
        log.warn("[REGRA DE NEGÓCIO] Violação de regra: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        pd.setTitle("Regra de negócio violada");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflito(ConflictException ex) {
        log.warn("[CONFLITO] Tentativa de operação duplicada: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Conflito de negócio");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public ProblemDetail handleProibido(Exception ex) {
        log.warn("[ACESSO NEGADO] Usuário sem permissão para realizar a ação: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        pd.setTitle("Acesso negado");
        pd.setDetail(ex.getMessage() != null ? ex.getMessage() : "Você não tem permissão para esta ação.");
        return pd;
    }

    @ExceptionHandler({UnauthorizedException.class, BadCredentialsException.class})
    public ProblemDetail handleNaoAutorizado(Exception ex) {
        log.warn("[NÃO AUTENTICADO] Credenciais inválidas ou token ausente/expirado: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        pd.setTitle("Não autenticado");
        pd.setDetail(ex.getMessage() != null ? ex.getMessage() : "Credenciais inválidas ou token expirado.");
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenerico(Exception ex) {
        log.error("[ERRO INTERNO] Exceção inesperada capturada — tipo: {} | mensagem: {}",
                ex.getClass().getSimpleName(), ex.getMessage(), ex);
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setTitle("Erro interno");
        pd.setDetail("Ocorreu um erro inesperado: " + ex.getMessage());
        return pd;
    }
}
