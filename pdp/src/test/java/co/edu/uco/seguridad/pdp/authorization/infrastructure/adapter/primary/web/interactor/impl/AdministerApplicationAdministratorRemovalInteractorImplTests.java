package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerApplicationAdministratorRemovalRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerApplicationAdministratorRemovalUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.RemoveApplicationAdministratorRawRequest;
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
 * HU-020: resuelve {@code tenantId} vía {@code ApplicationOwnerLookupValidator} a partir del
 * {@code applicationId} de la ruta — nunca del principal.
 */
class AdministerApplicationAdministratorRemovalInteractorImplTests {

    private static final String SUBJECT = "keycloak-subject-123";
    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final UserId CALLER = new UserId(UUID.randomUUID());
    private static final UserId TARGET = new UserId(UUID.randomUUID());

    @Test
    void resolves_the_tenant_from_the_application_owner_and_delegates() {
        List<AdministerApplicationAdministratorRemovalRequest> received = new ArrayList<>();
        AdministerApplicationAdministratorRemovalInteractorImpl interactor =
                new AdministerApplicationAdministratorRemovalInteractorImpl(ownerLookup(), subjectUserIdLookup(),
                        useCaseCapturing(received));
        RemoveApplicationAdministratorRawRequest raw =
                new RemoveApplicationAdministratorRawRequest(APPLICATION.value().toString(), TARGET.value().toString());

        StepVerifier.create(interactor.execute(raw).contextWrite(TestJwtSupport.withPrincipal(TENANT.value(), SUBJECT)))
                .verifyComplete();

        assertThat(received).hasSize(1);
        assertThat(received.getFirst().administration().tenantId()).isEqualTo(TENANT);
        assertThat(received.getFirst().administration().applicationId()).isEqualTo(APPLICATION);
        assertThat(received.getFirst().administration().subjectUserId()).isEqualTo(CALLER);
        assertThat(received.getFirst().removal().userId()).isEqualTo(TARGET);
        assertThat(received.getFirst().removal().tenantId()).isEqualTo(TENANT);
    }

    private static ApplicationOwnerLookupValidator ownerLookup() {
        return applicationId -> Mono.just(TENANT);
    }

    private static SubjectUserIdLookupValidator subjectUserIdLookup() {
        return subject -> Mono.just(CALLER);
    }

    private static AdministerApplicationAdministratorRemovalUseCase useCaseCapturing(
            List<AdministerApplicationAdministratorRemovalRequest> received) {
        return input -> {
            received.add(input);
            return Mono.empty();
        };
    }
}
