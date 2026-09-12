package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AccessDecisionRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AccessDecisionRawRequest.RawApplication;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AccessDecisionRawRequest.RawContext;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AccessDecisionRawRequest.RawResource;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.AccessDecisionInternalWebResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El controller solo delega — la lógica real (barreras C1-C4, resolución del tenant) vive en el
 * interactor y el puente, que sí están pendientes. No se envuelve en {@code ApiResponse} (D6).
 */
class InternalAccessDecisionControllerTests {

    @Test
    void evaluate_delegates_to_the_interactor_and_replies_with_200_unwrapped() {
        AccessDecisionInternalWebResponse expected = new AccessDecisionInternalWebResponse(
                "DENY", UUID.randomUUID().toString(), "TENANT_MISMATCH", List.of(), "req-1", "corr-1");
        InternalAccessDecisionController controller = new InternalAccessDecisionController(raw -> Mono.just(expected));
        AccessDecisionRawRequest body = new AccessDecisionRawRequest("1", "req-1", "corr-1",
                "2026-09-11T00:00:00Z", new RawApplication(UUID.randomUUID().toString(), "prod"),
                new RawResource("/estudiantes", "GET"), new RawContext("GET", "HTTP"));

        var response = controller.evaluate(body).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }
}
