package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.RegisterProtectedResourceBodyRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.AdministeredResourceWebResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** HU-017: misma ruta y código que el antiguo {@code ProtectedResourceController.register}. */
class ResourceAdministrationControllerTests {

    @Test
    void register_combines_the_path_variable_with_the_body_and_replies_with_201() {
        String applicationId = UUID.randomUUID().toString();
        AdministeredResourceWebResponse expected = new AdministeredResourceWebResponse(UUID.randomUUID().toString(),
                applicationId, "universidad-uco", "/estudiantes", "GET", Instant.now());
        ResourceAdministrationController controller = new ResourceAdministrationController(raw -> {
            assertThat(raw.applicationId()).isEqualTo(applicationId);
            assertThat(raw.path()).isEqualTo("/estudiantes");
            assertThat(raw.method()).isEqualTo("GET");
            return Mono.just(expected);
        });
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/applications/" + applicationId + "/resources").build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));

        var response = controller.register(applicationId,
                new RegisterProtectedResourceBodyRequest("/estudiantes", "GET"), exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().data()).isEqualTo(expected);
    }
}
