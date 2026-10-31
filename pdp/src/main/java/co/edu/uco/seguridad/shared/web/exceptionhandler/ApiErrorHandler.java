package co.edu.uco.seguridad.shared.web.exceptionhandler;

import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;
import co.edu.uco.seguridad.pdp.commons.exception.ConflictBusinessRuleException;
import co.edu.uco.seguridad.pdp.commons.exception.InvalidValueException;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import co.edu.uco.seguridad.shared.web.exception.RequestContractException;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;
import org.springframework.dao.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Locale;

/**
 * El único {@code @RestControllerAdvice} del proyecto (criterio 09). Los manejadores se declaran
 * contra los dos tipos base ({@link InvalidValueException}, {@link BusinessRuleViolationException}),
 * no contra cada excepción concreta, así que una regla nueva no necesita cambio aquí.
 */
@RestControllerAdvice
public class ApiErrorHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiErrorHandler.class);

    @ExceptionHandler(RequestContractException.class)
    ResponseEntity<ProblemDetail> requestContract(RequestContractException exception, ServerWebExchange exchange) {
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, exception.code(), exception.getMessage(), exchange);
        detail.setProperty("field", exception.field());
        return ResponseEntity.badRequest().body(detail);
    }

    @ExceptionHandler(InvalidValueException.class)
    ResponseEntity<ProblemDetail> invalidValue(InvalidValueException exception, ServerWebExchange exchange) {
        return ResponseEntity.badRequest()
                .body(problem(HttpStatus.BAD_REQUEST, exception.code(), exception.getMessage(), exchange));
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    ResponseEntity<ProblemDetail> businessRule(BusinessRuleViolationException exception, ServerWebExchange exchange) {
        HttpStatus status = exception instanceof ConflictBusinessRuleException
                ? HttpStatus.CONFLICT
                : "NOT_AUTHORIZED_TO_ADMINISTER".equals(exception.code())
                        || "NOT_AUTHORIZED_TO_ADMINISTER_TENANT_CATALOG".equals(exception.code())
                        || "CURRENT_USER_APPLICATIONS_ACCESS_DENIED".equals(exception.code())
                        || "MFA_REQUIRED".equals(exception.code())
                        ?HttpStatus.FORBIDDEN
                        : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(problem(status, exception.code(), exception.getMessage(), exchange));
    }

    @ExceptionHandler(ServerWebInputException.class)
    ResponseEntity<ProblemDetail> unreadableRequest(ServerWebInputException exception, ServerWebExchange exchange) {
        return ResponseEntity.badRequest().body(problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                WebContractMessages.unreadableBody(), exchange));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ProblemDetail> statusException(ResponseStatusException exception, ServerWebExchange exchange) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String code = "SESSION_EXPIRED".equals(exception.getReason())
                ? "SESSION_EXPIRED"
                : switch (status) {
                    case BAD_REQUEST -> "REQUEST_REJECTED";
                    case UNAUTHORIZED -> "UNAUTHORIZED";
                    case FORBIDDEN -> "FORBIDDEN";
                    case NOT_FOUND -> "RESOURCE_NOT_FOUND";
                    case SERVICE_UNAVAILABLE -> "SERVICE_UNAVAILABLE";
                    default -> status.is5xxServerError() ? "SERVICE_UNAVAILABLE" : "REQUEST_FAILED";
                };
        String detail = switch (status) {
            case BAD_REQUEST -> "La solicitud no pudo procesarse.";
            case UNAUTHORIZED -> "La sesión no es válida o expiró.";
            case FORBIDDEN -> "La operación no está autorizada.";
            case NOT_FOUND -> "El recurso solicitado no existe.";
            case SERVICE_UNAVAILABLE -> "Un servicio requerido no está disponible.";
            default -> status.is5xxServerError()
                    ? "Un servicio requerido no está disponible."
                    : "No se pudo procesar la solicitud.";
        };
        return ResponseEntity.status(status).body(problem(status, code, detail, exchange));
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ProblemDetail> dependencyUnavailable(DataAccessException exception, ServerWebExchange exchange) {
        LOG.atWarn().addKeyValue("event.name", "http.dependency.unavailable")
                .addKeyValue("error.type", exception.getClass().getSimpleName())
                .log("Falló una dependencia de persistencia");
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status)
                .body(problem(status, "DEPENDENCY_UNAVAILABLE", "Un servicio requerido no está disponible.", exchange));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> accessDenied(AccessDeniedException exception, ServerWebExchange exchange) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(problem(HttpStatus.FORBIDDEN, "ACCESS_DENIED", exception.getMessage(), exchange));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception exception, ServerWebExchange exchange) {
        LOG.atError().addKeyValue("event.name", "http.request.failed")
                .addKeyValue("error.type", exception.getClass().getSimpleName())
                .log("Error no manejado al atender la petición");
        return ResponseEntity.internalServerError().body(problem(HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR", WebContractMessages.internalError(), exchange));
    }

    private ProblemDetail problem(HttpStatus status, String code, String detail, ServerWebExchange exchange) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:security-baseline:error:" + code.toLowerCase(Locale.ROOT)));
        problem.setTitle(code);
        problem.setProperty("code", code);
        RequestContext context = CorrelationWebFilter.context(exchange);
        if (context != null) {
            problem.setProperty("requestId", context.requestId());
            problem.setProperty("correlationId", context.correlationId());
        }
        return problem;
    }
}
