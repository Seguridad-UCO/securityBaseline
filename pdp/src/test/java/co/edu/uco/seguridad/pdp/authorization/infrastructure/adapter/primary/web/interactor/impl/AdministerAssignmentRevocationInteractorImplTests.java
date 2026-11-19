package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignmentOwnershipQuery;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.AssignmentApplicationLookupValidator;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerAssignmentRevocationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerAssignmentRevocationUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.RevokeAssignmentRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.shared.security.TestJwtSupport;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-018: resuelve el {@code applicationId} vía {@code AssignmentApplicationLookupValidator} — la
 * petición de revocación no lo trae directo, a diferencia de la creación.
 */
class AdministerAssignmentRevocationInteractorImplTests {

    private static final String TENANT = "universidad-uco";
    private static final String SUBJECT = "keycloak-subject-123";
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final AssignmentId ASSIGNMENT = new AssignmentId(UUID.randomUUID());

    @Test
    void resolves_the_application_id_via_the_lookup_validator() {
        List<AdministerAssignmentRevocationRequest> received = new ArrayList<>();
        AdministerAssignmentRevocationInteractorImpl interactor = new AdministerAssignmentRevocationInteractorImpl(
                useCaseCapturing(received), subjectUserIdLookup(), applicationLookupResolving(APPLICATION));
        RevokeAssignmentRawRequest raw = new RevokeAssignmentRawRequest(ASSIGNMENT.value().toString());

        StepVerifier.create(interactor.execute(raw).contextWrite(TestJwtSupport.withPrincipal(TENANT, SUBJECT)))
                .verifyComplete();

        assertThat(received).hasSize(1);
        assertThat(received.getFirst().administration().applicationId()).isEqualTo(APPLICATION);
        assertThat(received.getFirst().administration().subjectUserId()).isEqualTo(USER);
        assertThat(received.getFirst().administration().tenantId().value()).isEqualTo(TENANT);
        assertThat(received.getFirst().revocation().assignmentId()).isEqualTo(ASSIGNMENT);
    }

    private static SubjectUserIdLookupValidator subjectUserIdLookup() {
        return subject -> Mono.just(USER);
    }

    private static AssignmentApplicationLookupValidator applicationLookupResolving(ApplicationId applicationId) {
        return (AssignmentOwnershipQuery query) -> Mono.just(applicationId);
    }

    private static AdministerAssignmentRevocationUseCase useCaseCapturing(List<AdministerAssignmentRevocationRequest> received) {
        return input -> {
            received.add(input);
            return Mono.empty();
        };
    }
}
