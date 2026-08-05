package co.edu.uco.seguridad.pdp.recursos.infrastructure.web;

import co.edu.uco.seguridad.pdp.aplicaciones.DuplicateApplicationException;
import co.edu.uco.seguridad.pdp.tenants.TenantUnavailableException;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import java.net.URI;

@RestControllerAdvice
final class ApiErrorHandler {
    @ExceptionHandler(DuplicateApplicationException.class) ResponseEntity<ProblemDetail> duplicate(DuplicateApplicationException ex, ServerWebExchange exchange) { return problem(HttpStatus.CONFLICT, "APPLICATION_ALREADY_EXISTS", ex.getMessage(), exchange); }
    @ExceptionHandler(TenantUnavailableException.class) ResponseEntity<ProblemDetail> tenant(TenantUnavailableException ex, ServerWebExchange exchange) { return problem(HttpStatus.BAD_REQUEST, "TENANT_NOT_ACTIVE", ex.getMessage(), exchange); }
    @ExceptionHandler({IllegalArgumentException.class, WebExchangeBindException.class}) ResponseEntity<ProblemDetail> validation(Exception ex, ServerWebExchange exchange) { return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request violates the catalog contract", exchange); }
    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail, ServerWebExchange exchange) { var p = ProblemDetail.forStatusAndDetail(status, detail); p.setType(URI.create("urn:security-baseline:error:" + code.toLowerCase())); p.setTitle(code); p.setProperty("code", code); var c = CorrelationWebFilter.context(exchange); p.setProperty("requestId", c.requestId()); p.setProperty("correlationId", c.correlationId()); return ResponseEntity.status(status).body(p); }
}
