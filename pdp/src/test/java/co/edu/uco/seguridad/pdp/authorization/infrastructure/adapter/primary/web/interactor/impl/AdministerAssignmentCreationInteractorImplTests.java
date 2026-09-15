package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerAssignmentCreationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerAssignmentCreationUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AssignRoleRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-018: construye el {@code AdministrationRequest} directamente desde el {@code applicationId} de
 * la propia petición (siempre presente) — sin ninguna consulta adicional, mismo criterio que
 * {@code AdministerResourceRegistrationInteractorImpl} (HU-017).
 */
class AdministerAssignmentCreationInteractorImplTests {

    private static final String TENANT = "universidad-uco";
    private static final String SUBJECT = "keycloak-subject-123";
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final RoleId ROLE = new RoleId(UUID.randomUUID());

    @Test
    void builds_the_administration_request_from_the_application_id_of_the_request() {
        List<AdministerAssignmentCreationRequest> received = new ArrayList<>();
        AssignmentResponse response = new AssignmentResponse(new AssignmentId(UUID.randomUUID()),
                new UserId(UUID.randomUUID()), new TenantId(TENANT), APPLICATION, ROLE,
                Instant.parse("2026-09-15T00:00:00Z"), Optional.empty());
        AdministerAssignmentCreationInteractorImpl interactor = new AdministerAssignmentCreationInteractorImpl(
                useCaseCapturing(received, response), subjectUserIdLookup());
        AssignRoleRawRequest raw = new AssignRoleRawRequest(ROLE.value().toString(),
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

    private static AdministerAssignmentCreationUseCase useCaseCapturing(
            List<AdministerAssignmentCreationRequest> received, AssignmentResponse response) {
        return input -> {
            received.add(input);
            return Mono.just(response);
        };
    }
}
