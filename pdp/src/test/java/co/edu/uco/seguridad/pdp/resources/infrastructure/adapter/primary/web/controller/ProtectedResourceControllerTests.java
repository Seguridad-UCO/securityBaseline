package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.response.ProtectedResourceWebResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-017: {@code register} se movió a {@code ResourceAdministrationController} (módulo
 * {@code authorization}) — su prueba vive ahora en {@code ResourceAdministrationControllerTests}.
 * Este controller solo conserva la consulta.
 */
class ProtectedResourceControllerTests {

    @Test
    void list_passes_the_path_variable_through_and_replies_with_200() {
        String applicationId = UUID.randomUUID().toString();
        ProtectedResourceWebResponse resource = new ProtectedResourceWebResponse(UUID.randomUUID().toString(),
                applicationId, "universidad-uco", "/estudiantes", "GET", Instant.now());
        ProtectedResourceController controller = new ProtectedResourceController(
                rawAppId -> {
                    assertThat(rawAppId).isEqualTo(applicationId);
                    return Mono.just(List.of(resource));
                });
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/applications/" + applicationId + "/resources").build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));

        var response = controller.list(applicationId, exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).containsExactly(resource);
    }
}
