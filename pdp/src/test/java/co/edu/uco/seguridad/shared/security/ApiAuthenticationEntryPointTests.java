package co.edu.uco.seguridad.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Se ejecuta cuando el token está ausente, mal formado o expirado — antes de llegar a un
 * controlador (ver la clase bajo prueba). Mismas dos rutas que {@link ApiAccessDeniedHandlerTests}:
 * serialización real y el fallback fijo cuando falla, algo que la cadena de filtros completa nunca
 * ejercita.
 */
class ApiAuthenticationEntryPointTests {

    @Test
    void writes_an_unauthorized_problem_detail() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/protected-applications"));

        new ApiAuthenticationEntryPoint(new ObjectMapper())
                .commence(exchange, new BadCredentialsException("no token"))
                .block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("\"code\":\"UNAUTHORIZED\"");
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

        new ApiAuthenticationEntryPoint(failingMapper)
                .commence(exchange, new BadCredentialsException("no token"))
                .block();

        assertThat(exchange.getResponse().getBodyAsString().block())
                .isEqualTo("{\"detail\":\"unauthorized\"}");
    }
}
