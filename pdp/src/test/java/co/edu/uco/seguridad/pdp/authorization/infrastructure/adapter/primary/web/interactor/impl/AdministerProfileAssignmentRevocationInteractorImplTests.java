package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ProfileAssignmentOwnershipQuery;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.ProfileAssignmentApplicationLookupValidator;
import co.edu.uco.seguridad.pdp.assignments.domain.model.ProfileAssignmentId;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerProfileAssignmentRevocationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerProfileAssignmentRevocationUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.RevokeProfileAssignmentRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
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
 * HU-019: resuelve el {@code applicationId} vía {@code ProfileAssignmentApplicationLookupValidator}
 * — la petición de revocación no lo trae directo, mismo criterio que
 * {@code AdministerAssignmentRevocationInteractorImpl} (HU-018).
 */
class AdministerProfileAssignmentRevocationInteractorImplTests {

    private static final String TENANT = "universidad-uco";
    private static final String SUBJECT = "keycloak-subject-123";
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final ProfileAssignmentId PROFILE_ASSIGNMENT = new ProfileAssignmentId(UUID.randomUUID());

    @Test
    void resolves_the_application_id_via_the_lookup_validator() {
        List<AdministerProfileAssignmentRevocationRequest> received = new ArrayList<>();
        AdministerProfileAssignmentRevocationInteractorImpl interactor = new AdministerProfileAssignmentRevocationInteractorImpl(
                useCaseCapturing(received), subjectUserIdLookup(), applicationLookupResolving(APPLICATION));
        RevokeProfileAssignmentRawRequest raw = new RevokeProfileAssignmentRawRequest(PROFILE_ASSIGNMENT.value().toString());

        StepVerifier.create(interactor.execute(raw).contextWrite(TestJwtSupport.withPrincipal(TENANT, SUBJECT)))
                .verifyComplete();

        assertThat(received).hasSize(1);
        assertThat(received.getFirst().administration().applicationId()).isEqualTo(APPLICATION);
        assertThat(received.getFirst().administration().subjectUserId()).isEqualTo(USER);
        assertThat(received.getFirst().administration().tenantId().value()).isEqualTo(TENANT);
        assertThat(received.getFirst().revocation().profileAssignmentId()).isEqualTo(PROFILE_ASSIGNMENT);
    }

    private static SubjectUserIdLookupValidator subjectUserIdLookup() {
        return subject -> Mono.just(USER);
    }

    private static ProfileAssignmentApplicationLookupValidator applicationLookupResolving(ApplicationId applicationId) {
        return (ProfileAssignmentOwnershipQuery query) -> Mono.just(applicationId);
    }

    private static AdministerProfileAssignmentRevocationUseCase useCaseCapturing(
            List<AdministerProfileAssignmentRevocationRequest> received) {
        return input -> {
            received.add(input);
            return Mono.empty();
        };
    }
}
