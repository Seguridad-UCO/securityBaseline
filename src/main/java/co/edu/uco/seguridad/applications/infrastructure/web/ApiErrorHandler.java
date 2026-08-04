package co.edu.uco.seguridad.applications.infrastructure.web;

import co.edu.uco.seguridad.applications.domain.DomainException;
import co.edu.uco.seguridad.applications.domain.DuplicateProtectedApplicationException;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import java.net.URI;
import java.time.Instant;

@RestControllerAdvice
final class ApiErrorHandler {
    @ExceptionHandler(DuplicateProtectedApplicationException.class)
    ResponseEntity<ProblemDetail> duplicate(DuplicateProtectedApplicationException ex, ServerWebExchange exchange) { return problem(HttpStatus.CONFLICT, "APPLICATION_ALREADY_EXISTS", ex.getMessage(), exchange); }
    @ExceptionHandler({DomainException.class, IllegalArgumentException.class, WebExchangeBindException.class})
    ResponseEntity<ProblemDetail> validation(Exception ex, ServerWebExchange exchange) { return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request violates the application contract", exchange); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception ex, ServerWebExchange exchange) { return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", exchange); }
    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail, ServerWebExchange exchange) {
        var context = CorrelationWebFilter.context(exchange);
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:security-baseline:error:" + code.toLowerCase()));
        problem.setTitle(code); problem.setProperty("code", code); problem.setProperty("timestamp", Instant.now());
        problem.setProperty("requestId", context.requestId()); problem.setProperty("correlationId", context.correlationId());
        return ResponseEntity.status(status).body(problem);
    }
}
