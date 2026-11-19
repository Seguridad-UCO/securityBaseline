package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.TenantAdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerTenantCatalogException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.usecase.ListUsersUseCase;
import co.edu.uco.seguridad.shared.security.mfa.AuthenticationContextEvidence;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SU-01: solo un actor de inquilino ve el catálogo de usuarios, y solo el de su inquilino.
 */
class AdministerUserListingUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final TenantAdministrationRequest ACTOR = new TenantAdministrationRequest(TENANT,
            new UserId(UUID.randomUUID()), "actor-subject", AuthenticationContextEvidence.NONE);

    @Test
    void a_tenant_actor_lists_the_users_of_their_own_tenant() {
        AtomicReference<TenantId> listed = new AtomicReference<>();
        ListUsersUseCase listUsers = tenant -> Mono.fromCallable(() -> {
            listed.set(tenant);
            return List.of();
        });

        StepVerifier.create(new AdministerUserListingUseCaseImpl(actor -> Mono.just(new ApplicationId(UUID.randomUUID())),
                        listUsers).execute(ACTOR))
                .expectNext(List.of())
                .verifyComplete();

        assertThat(listed).hasValue(TENANT);
    }

    @Test
    void a_principal_that_administers_no_application_is_rejected_and_nothing_is_read() {
        ListUsersUseCase unreachable = tenant -> Mono.error(new AssertionError("no debía listar"));

        StepVerifier.create(new AdministerUserListingUseCaseImpl(
                        actor -> Mono.error(new NotAuthorizedToAdministerTenantCatalogException(TENANT, Optional.empty())),
                        unreachable).execute(ACTOR))
                .expectError(NotAuthorizedToAdministerTenantCatalogException.class)
                .verify();
    }
}
