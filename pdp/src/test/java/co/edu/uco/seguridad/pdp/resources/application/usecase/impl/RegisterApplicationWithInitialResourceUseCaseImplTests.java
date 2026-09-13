package co.edu.uco.seguridad.pdp.resources.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.ApplicationRegistrationResponse;
import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RegisterApplicationUseCase;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RemoveApplicationUseCase;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.RegisterApplicationWithInitialResourceRequest;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.RegisterProtectedResourceRequest;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.response.RegisteredProtectedResourceResponse;
import co.edu.uco.seguridad.pdp.resources.application.usecase.RegisterProtectedResourceUseCase;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La saga no decide nada de negocio: orquesta y compensa. Cada caso comprueba una traducción o una
 * compensación, nunca una regla — mismo espíritu que HU-007/HU-013.
 */
class RegisterApplicationWithInitialResourceUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION_ID = new ApplicationId(UUID.randomUUID());
    private static final ApplicationName NAME = new ApplicationName("gestion-academica");
    private static final ApplicationBaseUrl BASE_URL = new ApplicationBaseUrl("https://example.com");
    private static final ResourcePath RESOURCE_PATH = new ResourcePath("/estudiantes");
    private static final HttpVerb METHOD = HttpVerb.GET;
    private static final Instant REGISTERED_AT = Instant.parse("2026-09-13T00:00:00Z");
    private static final ResourceId RESOURCE_ID = new ResourceId(UUID.randomUUID());
    private static final RegisterApplicationWithInitialResourceRequest REQUEST =
            new RegisterApplicationWithInitialResourceRequest(TENANT, NAME, "Sistema académico", BASE_URL,
                    RESOURCE_PATH, METHOD);

    @Test
    void registers_the_application_and_its_initial_resource_returning_both() {
        List<RegisterProtectedResourceRequest> received = new ArrayList<>();
        RegisterApplicationWithInitialResourceUseCaseImpl useCase = new RegisterApplicationWithInitialResourceUseCaseImpl(
                registerApplicationSucceeding(), registerResourceSucceeding(received), neverCompensate());

        StepVerifier.create(useCase.execute(REQUEST))
                .assertNext(response -> {
                    assertThat(response.application().credential()).isEqualTo("secreto-en-claro");
                    assertThat(response.application().application().id()).isEqualTo(APPLICATION_ID);
                    assertThat(response.resource().applicationId()).isEqualTo(APPLICATION_ID);
                    assertThat(response.resource().path()).isEqualTo(RESOURCE_PATH);
                })
                .verifyComplete();

        assertThat(received).hasSize(1);
        assertThat(received.getFirst().applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(received.getFirst().tenantId()).isEqualTo(TENANT);
    }

    @Test
    void never_registers_a_resource_or_compensates_when_the_application_registration_fails() {
        RuntimeException failure = new RuntimeException("nombre reservado");
        RegisterApplicationUseCase registerApplication = request -> Mono.error(failure);
        RegisterProtectedResourceUseCase registerResource =
                request -> { throw new AssertionError("must not reach resource registration"); };
        RemoveApplicationUseCase removeApplication = neverCompensate();
        RegisterApplicationWithInitialResourceUseCaseImpl useCase = new RegisterApplicationWithInitialResourceUseCaseImpl(
                registerApplication, registerResource, removeApplication);

        StepVerifier.create(useCase.execute(REQUEST)).expectErrorMessage("nombre reservado").verify();
    }

    @Test
    void compensates_by_removing_the_application_when_the_resource_registration_fails() {
        RuntimeException resourceFailure = new RuntimeException("recurso inválido");
        List<ApplicationId> removed = new ArrayList<>();
        RegisterApplicationUseCase registerApplication = registerApplicationSucceeding();
        RegisterProtectedResourceUseCase registerResource = request -> Mono.error(resourceFailure);
        RemoveApplicationUseCase removeApplication = id -> {
            removed.add(id);
            return Mono.empty();
        };
        RegisterApplicationWithInitialResourceUseCaseImpl useCase = new RegisterApplicationWithInitialResourceUseCaseImpl(
                registerApplication, registerResource, removeApplication);

        StepVerifier.create(useCase.execute(REQUEST)).expectErrorMessage("recurso inválido").verify();

        assertThat(removed).containsExactly(APPLICATION_ID);
    }

    @Test
    void still_reports_the_original_resource_error_when_the_compensation_itself_fails() {
        RuntimeException resourceFailure = new RuntimeException("recurso inválido");
        RuntimeException compensationFailure = new RuntimeException("surrealdb no disponible");
        RegisterApplicationUseCase registerApplication = registerApplicationSucceeding();
        RegisterProtectedResourceUseCase registerResource = request -> Mono.error(resourceFailure);
        RemoveApplicationUseCase removeApplication = id -> Mono.error(compensationFailure);
        RegisterApplicationWithInitialResourceUseCaseImpl useCase = new RegisterApplicationWithInitialResourceUseCaseImpl(
                registerApplication, registerResource, removeApplication);

        StepVerifier.create(useCase.execute(REQUEST)).expectErrorMessage("recurso inválido").verify();
    }

    private static RegisterApplicationUseCase registerApplicationSucceeding() {
        return request -> Mono.just(new ApplicationRegistrationResponse(
                new RegisteredApplicationResponse(APPLICATION_ID, TENANT, NAME, "Sistema académico", BASE_URL,
                        REGISTERED_AT),
                "secreto-en-claro"));
    }

    private static RegisterProtectedResourceUseCase registerResourceSucceeding(List<RegisterProtectedResourceRequest> received) {
        return request -> {
            received.add(request);
            return Mono.just(new RegisteredProtectedResourceResponse(RESOURCE_ID, APPLICATION_ID, TENANT,
                    RESOURCE_PATH, METHOD, REGISTERED_AT));
        };
    }

    private static RemoveApplicationUseCase neverCompensate() {
        return id -> { throw new AssertionError("must not reach compensation"); };
    }
}
