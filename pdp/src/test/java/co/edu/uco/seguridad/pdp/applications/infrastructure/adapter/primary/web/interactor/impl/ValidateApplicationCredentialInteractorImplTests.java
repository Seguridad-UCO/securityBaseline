package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ValidateApplicationCredentialRequest;
import co.edu.uco.seguridad.pdp.applications.application.usecase.ValidateApplicationCredentialUseCase;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw.ValidateApplicationCredentialRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sin barreras de {@code requestId}/{@code correlationId} ni sujeto de evidencia (a diferencia de
 * {@code InternalAccessDecisionInteractorImpl}): este canal no tiene "quién pregunta" (Hallazgo 1
 * del plan de HU-013), solo mapea, delega y mapea la respuesta.
 */
class ValidateApplicationCredentialInteractorImplTests {

    @Test
    void maps_the_raw_request_delegates_to_the_use_case_and_maps_the_response() {
        String applicationId = UUID.randomUUID().toString();
        TenantId tenant = new TenantId("universidad-uco");
        List<ValidateApplicationCredentialRequest> received = new ArrayList<>();
        ValidateApplicationCredentialUseCase useCase = request -> {
            received.add(request);
            return Mono.just(tenant);
        };
        ValidateApplicationCredentialInteractorImpl interactor = new ValidateApplicationCredentialInteractorImpl(useCase);

        StepVerifier.create(interactor.execute(new ValidateApplicationCredentialRawRequest(applicationId, "secreto")))
                .assertNext(response -> assertThat(response.tenantId()).isEqualTo("universidad-uco"))
                .verifyComplete();

        assertThat(received).hasSize(1);
        assertThat(received.get(0).applicationId()).isEqualTo(ApplicationId.of(applicationId));
        assertThat(received.get(0).secret()).isEqualTo("secreto");
    }
}
