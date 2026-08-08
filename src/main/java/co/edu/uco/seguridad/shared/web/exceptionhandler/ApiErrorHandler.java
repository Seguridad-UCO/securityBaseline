package co.edu.uco.seguridad.shared.web.exceptionhandler;

import co.edu.uco.seguridad.crosscutting.messages.WebContractMessages;
import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;
import co.edu.uco.seguridad.pdp.commons.exception.ConflictBusinessRuleException;
import co.edu.uco.seguridad.pdp.commons.exception.InvalidValueException;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import co.edu.uco.seguridad.shared.web.exception.RequestContractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import java.net.URI;
import java.util.Locale;

/**
 * El handler global de excepciones de la aplicación (adaptador primario/driving): el único lugar
 * donde un fallo se convierte en un estado HTTP (criterio 09). No hay ningún otro
 * {@code @RestControllerAdvice} en el proyecto — este es el único.
 *
 * <p>Los manejadores se declaran contra los dos tipos base ({@link InvalidValueException},
 * {@link BusinessRuleViolationException}) en lugar de contra cada excepción concreta, por lo que agregar
 * una regla nueva no necesita cambio aquí: la excepción ya lleva su propio {@code código} estable. El
 * estado se elige por significado — una solicitud malformada es 400, un registro repetido
 * ({@link ConflictBusinessRuleException}) es 409, cualquier cosa imprevista es un bare 500 cuyo detalle
 * es deliberadamente genérico, con la verdadera causa yendo al registro y no al cliente.</p>
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
                : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(problem(status, exception.code(), exception.getMessage(), exchange));
    }

    @ExceptionHandler(ServerWebInputException.class)
    ResponseEntity<ProblemDetail> unreadableRequest(ServerWebInputException exception, ServerWebExchange exchange) {
        return ResponseEntity.badRequest().body(problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                WebContractMessages.unreadableBody(), exchange));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception exception, ServerWebExchange exchange) {
        LOG.error("error no manejado mientras se sirve {}", exchange.getRequest().getPath(), exception);
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
