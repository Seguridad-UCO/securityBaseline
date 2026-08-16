package co.edu.uco.seguridad.shared.security;

import co.edu.uco.seguridad.crosscutting.messages.RequiredArgumentMessages;
import co.edu.uco.seguridad.crosscutting.messages.WebContractMessages;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Token ausente, mal formado, inválido o expirado. Devuelve el mismo {@link ProblemDetail} que
 * {@code ApiErrorHandler}, aunque no puede ser un {@code @ExceptionHandler} suyo: el rechazo ocurre
 * en la cadena de filtros de seguridad, antes de llegar a un controlador.
 */
public final class ApiAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    private final ObjectMapper mapper;

    public ApiAuthenticationEntryPoint(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, RequiredArgumentMessages.OBJECT_MAPPER);
    }

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, WebContractMessages.unauthorized());
        problem.setType(URI.create("urn:security-baseline:error:unauthorized"));
        problem.setTitle("UNAUTHORIZED");
        problem.setProperty("code", "UNAUTHORIZED");

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return response.writeWith(Mono.fromSupplier(() -> {
            try {
                return response.bufferFactory().wrap(mapper.writeValueAsBytes(problem));
            } catch (Exception cause) {
                return response.bufferFactory().wrap(
                        "{\"detail\":\"unauthorized\"}".getBytes(StandardCharsets.UTF_8));
            }
        }));
    }
}
