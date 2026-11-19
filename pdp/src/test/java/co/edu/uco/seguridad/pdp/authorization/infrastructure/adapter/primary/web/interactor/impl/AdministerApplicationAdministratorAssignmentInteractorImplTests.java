package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerApplicationAdministratorAssignmentRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerApplicationAdministratorAssignmentUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AssignApplicationAdministratorRawRequest;
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
 * HU-020: resuelve {@code tenantId} vía {@link ApplicationOwnerLookupValidator} a partir del
 * {@code applicationId} de la ruta — nunca del principal.
 */
class AdministerApplicationAdministratorAssignmentInteractorImplTests {

    private static final String SUBJECT = "keycloak-subject-123";
    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final UserId CALLER = new UserId(UUID.randomUUID());
    private static final UserId NEW_ADMINISTRATOR = new UserId(UUID.randomUUID());

    @Test
    void resolves_the_tenant_from_the_application_owner_and_delegates() {
        List<AdministerApplicationAdministratorAssignmentRequest> received = new ArrayList<>();
        AssignmentResponse response = new AssignmentResponse(new AssignmentId(UUID.randomUUID()), NEW_ADMINISTRATOR, TENANT,
                APPLICATION, new RoleId(UUID.randomUUID()), Instant.parse("2026-09-15T00:00:00Z"), Optional.empty());
        AdministerApplicationAdministratorAssignmentInteractorImpl interactor =
                new AdministerApplicationAdministratorAssignmentInteractorImpl(ownerLookup(), subjectUserIdLookup(),
                        useCaseCapturing(received, response));
        AssignApplicationAdministratorRawRequest raw =
                new AssignApplicationAdministratorRawRequest(APPLICATION.value().toString(),
                        NEW_ADMINISTRATOR.value().toString());

        StepVerifier.create(interactor.execute(raw).contextWrite(TestJwtSupport.withPrincipal(TENANT.value(), SUBJECT)))
                .assertNext(webResponse -> assertThat(webResponse.userId())
                        .isEqualTo(NEW_ADMINISTRATOR.value().toString()))
                .verifyComplete();

        assertThat(received).hasSize(1);
        assertThat(received.getFirst().administration().tenantId()).isEqualTo(TENANT);
        assertThat(received.getFirst().administration().applicationId()).isEqualTo(APPLICATION);
        assertThat(received.getFirst().administration().subjectUserId()).isEqualTo(CALLER);
        assertThat(received.getFirst().assignment().userId()).isEqualTo(NEW_ADMINISTRATOR);
        assertThat(received.getFirst().assignment().tenantId()).isEqualTo(TENANT);
    }

    private static ApplicationOwnerLookupValidator ownerLookup() {
        return applicationId -> Mono.just(TENANT);
    }

    private static SubjectUserIdLookupValidator subjectUserIdLookup() {
        return subject -> Mono.just(CALLER);
    }

    private static AdministerApplicationAdministratorAssignmentUseCase useCaseCapturing(
            List<AdministerApplicationAdministratorAssignmentRequest> received, AssignmentResponse response) {
        return input -> {
            received.add(input);
            return Mono.just(response);
        };
    }
}
