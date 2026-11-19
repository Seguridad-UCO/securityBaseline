package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerApplicationAdministratorListRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerApplicationAdministratorListUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ListApplicationAdministratorsRawRequest;
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
 * HU-020: resuelve {@code tenantId} vía {@code ApplicationOwnerLookupValidator} a partir del
 * {@code applicationId} de la ruta — nunca del principal.
 */
class AdministerApplicationAdministratorListInteractorImplTests {

    private static final String SUBJECT = "keycloak-subject-123";
    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final UserId CALLER = new UserId(UUID.randomUUID());

    @Test
    void resolves_the_tenant_from_the_application_owner_and_delegates() {
        List<AdministerApplicationAdministratorListRequest> received = new ArrayList<>();
        AssignmentResponse administrator = new AssignmentResponse(new AssignmentId(UUID.randomUUID()), CALLER, TENANT,
                APPLICATION, new RoleId(UUID.randomUUID()), Instant.parse("2026-09-15T00:00:00Z"), Optional.empty());
        AdministerApplicationAdministratorListInteractorImpl interactor =
                new AdministerApplicationAdministratorListInteractorImpl(ownerLookup(), subjectUserIdLookup(),
                        useCaseCapturing(received, List.of(administrator)));
        ListApplicationAdministratorsRawRequest raw =
                new ListApplicationAdministratorsRawRequest(APPLICATION.value().toString());

        StepVerifier.create(interactor.execute(raw).contextWrite(TestJwtSupport.withPrincipal(TENANT.value(), SUBJECT)))
                .assertNext(webResponses -> assertThat(webResponses).hasSize(1)
                        .first()
                        .extracting("userId")
                        .isEqualTo(CALLER.value().toString()))
                .verifyComplete();

        assertThat(received).hasSize(1);
        assertThat(received.getFirst().administration().tenantId()).isEqualTo(TENANT);
        assertThat(received.getFirst().administration().applicationId()).isEqualTo(APPLICATION);
        assertThat(received.getFirst().query().tenantId()).isEqualTo(TENANT);
        assertThat(received.getFirst().query().applicationId()).isEqualTo(APPLICATION);
    }

    private static ApplicationOwnerLookupValidator ownerLookup() {
        return applicationId -> Mono.just(TENANT);
    }

    private static SubjectUserIdLookupValidator subjectUserIdLookup() {
        return subject -> Mono.just(CALLER);
    }

    private static AdministerApplicationAdministratorListUseCase useCaseCapturing(
            List<AdministerApplicationAdministratorListRequest> received, List<AssignmentResponse> administrators) {
        return input -> {
            received.add(input);
            return Mono.just(administrators);
        };
    }
}
