package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerResourceRegistrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerResourceRegistrationUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.RegisterProtectedResourceRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.response.RegisteredProtectedResourceResponse;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import co.edu.uco.seguridad.shared.security.TestJwtSupport;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-017: construye el {@code AdministrationRequest} directamente desde el {@code applicationId} de
 * la propia petición (siempre presente) — sin ninguna consulta adicional, a diferencia de
 * {@code AdministerResourceGrantInteractorImpl} (HU-016).
 */
class AdministerResourceRegistrationInteractorImplTests {

    private static final String TENANT = "universidad-uco";
    private static final String SUBJECT = "keycloak-subject-123";
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final ResourceId RESOURCE_ID = new ResourceId(UUID.randomUUID());
    private static final Instant REGISTERED_AT = Instant.parse("2026-09-14T00:00:00Z");

    @Test
    void builds_the_administration_request_from_the_application_id_of_the_request() {
        List<AdministerResourceRegistrationRequest> received = new ArrayList<>();
        RegisteredProtectedResourceResponse response = new RegisteredProtectedResourceResponse(RESOURCE_ID,
                APPLICATION, new TenantId(TENANT), new ResourcePath("/estudiantes"), HttpVerb.GET, REGISTERED_AT);
        AdministerResourceRegistrationInteractorImpl interactor = new AdministerResourceRegistrationInteractorImpl(
                useCaseCapturing(received, response), subjectUserIdLookup());
        RegisterProtectedResourceRawRequest raw =
                new RegisterProtectedResourceRawRequest(APPLICATION.value().toString(), "/estudiantes", "GET");

        StepVerifier.create(interactor.execute(raw).contextWrite(TestJwtSupport.withPrincipal(TENANT, SUBJECT)))
                .assertNext(webResponse -> assertThat(webResponse.id()).isEqualTo(RESOURCE_ID.value().toString()))
                .verifyComplete();

        assertThat(received).hasSize(1);
        assertThat(received.getFirst().administration().applicationId()).isEqualTo(APPLICATION);
        assertThat(received.getFirst().administration().subjectUserId()).isEqualTo(USER);
        assertThat(received.getFirst().administration().tenantId().value()).isEqualTo(TENANT);
        assertThat(received.getFirst().resource().applicationId()).isEqualTo(APPLICATION);
    }

    private static SubjectUserIdLookupValidator subjectUserIdLookup() {
        return subject -> Mono.just(USER);
    }

    private static AdministerResourceRegistrationUseCase useCaseCapturing(
            List<AdministerResourceRegistrationRequest> received, RegisteredProtectedResourceResponse response) {
        return input -> {
            received.add(input);
            return Mono.just(response);
        };
    }
}
