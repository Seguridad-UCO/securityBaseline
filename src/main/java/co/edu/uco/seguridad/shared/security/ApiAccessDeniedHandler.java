package co.edu.uco.seguridad.shared.security;

import co.edu.uco.seguridad.crosscutting.messages.WebContractMessages;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Token válido, pero la autorización lo rechaza. Ninguna regla de este tipo existe todavía —hoy
 * "autenticado" basta para operar— pero el handler se registra desde ya para que añadir una regla de
 * autorización más adelante (p. ej. un scope por operación) no requiera tocar el contrato de error.
 */
public final class ApiAccessDeniedHandler implements ServerAccessDeniedHandler {

    private final ObjectMapper mapper;

    public ApiAccessDeniedHandler(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "se requiere ObjectMapper");
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, WebContractMessages.forbidden());
        problem.setType(URI.create("urn:security-baseline:error:forbidden"));
        problem.setTitle("FORBIDDEN");
        problem.setProperty("code", "FORBIDDEN");

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return response.writeWith(Mono.fromSupplier(() -> {
            try {
                return response.bufferFactory().wrap(mapper.writeValueAsBytes(problem));
            } catch (Exception cause) {
                return response.bufferFactory().wrap(
                        "{\"detail\":\"forbidden\"}".getBytes(StandardCharsets.UTF_8));
            }
        }));
    }
}
