package co.edu.uco.seguridad.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Se ejecuta cuando la autorización rechaza un token válido (ver la clase bajo prueba para por qué
 * eso todavía no puede pasar en producción). Cubre las dos rutas: el {@link tools.jackson.databind.ObjectMapper}
 * real serializa el problem detail, y el fallback fijo cuando serializar falla — algo que la cadena
 * de filtros completa nunca ejercita porque el mapper real nunca falla con un {@code ProblemDetail}
 * bien formado.
 */
class ApiAccessDeniedHandlerTests {

    @Test
    void writes_a_forbidden_problem_detail() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/protected-applications"));

        new ApiAccessDeniedHandler(new ObjectMapper())
                .handle(exchange, new AccessDeniedException("denied"))
                .block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("\"code\":\"FORBIDDEN\"");
    }

    @Test
    void falls_back_to_a_fixed_body_when_serialization_fails() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/protected-applications"));
        ObjectMapper failingMapper = new ObjectMapper() {
            @Override
            public byte[] writeValueAsBytes(Object value) {
                throw new RuntimeException("serialization is down");
            }
        };

        new ApiAccessDeniedHandler(failingMapper)
                .handle(exchange, new AccessDeniedException("denied"))
                .block();

        assertThat(exchange.getResponse().getBodyAsString().block())
                .isEqualTo("{\"detail\":\"forbidden\"}");
    }
}
