package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ProfileAssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.domain.model.ProfileAssignmentId;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerProfileAssignmentCreationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerProfileAssignmentCreationUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AssignProfileRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.shared.security.TestJwtSupport;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-019: construye el {@code AdministrationRequest} directamente desde el {@code applicationId} de
 * la propia petición (siempre presente) — mismo criterio que {@code AdministerAssignmentCreationInteractorImpl} (HU-018).
 */
class AdministerProfileAssignmentCreationInteractorImplTests {

    private static final String TENANT = "universidad-uco";
    private static final String SUBJECT = "keycloak-subject-123";
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final ProfileId PROFILE = new ProfileId(UUID.randomUUID());

    @Test
    void builds_the_administration_request_from_the_application_id_of_the_request() {
        List<AdministerProfileAssignmentCreationRequest> received = new ArrayList<>();
        ProfileAssignmentResponse response = new ProfileAssignmentResponse(new ProfileAssignmentId(UUID.randomUUID()),
                new UserId(UUID.randomUUID()), new TenantId(TENANT), APPLICATION, PROFILE, Set.of(),
                Instant.parse("2026-09-15T00:00:00Z"), Optional.empty());
        AdministerProfileAssignmentCreationInteractorImpl interactor = new AdministerProfileAssignmentCreationInteractorImpl(
                useCaseCapturing(received, response), subjectUserIdLookup());
        AssignProfileRawRequest raw = new AssignProfileRawRequest(PROFILE.value().toString(),
                response.userId().value().toString(), APPLICATION.value().toString());

        StepVerifier.create(interactor.execute(raw).contextWrite(TestJwtSupport.withPrincipal(TENANT, SUBJECT)))
                .assertNext(webResponse -> assertThat(webResponse.id()).isEqualTo(response.id().value().toString()))
                .verifyComplete();

        assertThat(received).hasSize(1);
        assertThat(received.getFirst().administration().applicationId()).isEqualTo(APPLICATION);
        assertThat(received.getFirst().administration().subjectUserId()).isEqualTo(USER);
        assertThat(received.getFirst().administration().tenantId().value()).isEqualTo(TENANT);
        assertThat(received.getFirst().assignment().applicationId()).isEqualTo(APPLICATION);
    }

    private static SubjectUserIdLookupValidator subjectUserIdLookup() {
        return subject -> Mono.just(USER);
    }

    private static AdministerProfileAssignmentCreationUseCase useCaseCapturing(
            List<AdministerProfileAssignmentCreationRequest> received, ProfileAssignmentResponse response) {
        return input -> {
            received.add(input);
            return Mono.just(response);
        };
    }
}
