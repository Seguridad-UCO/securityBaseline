package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw.ValidateApplicationCredentialRawRequest;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.response.ApplicationCredentialValidationWebResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InternalApplicationCredentialControllerTests {

    private static final ApplicationCredentialValidationWebResponse EXPECTED =
            new ApplicationCredentialValidationWebResponse("universidad-uco");

    @Test
    void validate_uses_the_application_id_from_the_route_not_the_body_and_replies_with_200() {
        List<ValidateApplicationCredentialRawRequest> received = new ArrayList<>();
        InternalApplicationCredentialController controller = new InternalApplicationCredentialController(raw -> {
            received.add(raw);
            return Mono.just(EXPECTED);
        });

        var response = controller.validate("app-from-route",
                new ValidateApplicationCredentialRawRequest("app-from-body", "secreto")).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(EXPECTED);
        assertThat(received).hasSize(1);
        assertThat(received.get(0).applicationName()).isEqualTo("app-from-route");
        assertThat(received.get(0).secret()).isEqualTo("secreto");
    }
}
