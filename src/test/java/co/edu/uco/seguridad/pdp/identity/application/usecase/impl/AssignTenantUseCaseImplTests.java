package co.edu.uco.seguridad.pdp.identity.application.usecase.impl;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.identity.application.exception.UserNotFoundException;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.request.AssignTenantRequest;
import co.edu.uco.seguridad.pdp.identity.application.secondaryport.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.identity.domain.Email;
import co.edu.uco.seguridad.pdp.identity.domain.ExternalIdentity;
import co.edu.uco.seguridad.pdp.identity.domain.SecurityUser;
import co.edu.uco.seguridad.pdp.identity.domain.UserId;
import co.edu.uco.seguridad.pdp.tenants.application.primaryport.response.TenantResponse;
import co.edu.uco.seguridad.pdp.tenants.domain.TenantName;
import co.edu.uco.seguridad.pdp.tenants.domain.TenantStatus;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AssignTenantUseCaseImplTests {

    private static final TenantId NEW_TENANT = new TenantId("otra-universidad");

    @Test
    void reassigns_an_existing_user_to_the_confirmed_active_tenant() {
        SecurityUser user = SecurityUser.provision(new UserId(UUID.randomUUID()), new TenantId("universidad-uco"),
                new Email("david@uco.edu"), "David", Instant.now());
        AssignTenantUseCaseImpl useCase = new AssignTenantUseCaseImpl(
                tenantId -> Mono.just(new TenantResponse(tenantId, new TenantName("Otra"), TenantStatus.ACTIVE)),
                userId -> Mono.just(user),
                repositoryWithUser(user));

        StepVerifier.create(useCase.execute(new AssignTenantRequest(user.id(), NEW_TENANT)))
                .assertNext(response -> assertThat(response.tenantId()).isEqualTo(NEW_TENANT))
                .verifyComplete();
    }

    @Test
    void refuses_to_reassign_a_user_that_does_not_exist() {
        UserId missing = new UserId(UUID.randomUUID());
        AssignTenantUseCaseImpl useCase = new AssignTenantUseCaseImpl(
                tenantId -> Mono.just(new TenantResponse(tenantId, new TenantName("Otra"), TenantStatus.ACTIVE)),
                userId -> Mono.error(new UserNotFoundException(userId)),
                repositoryWithUser(null));

        StepVerifier.create(useCase.execute(new AssignTenantRequest(missing, NEW_TENANT)))
                .expectError(UserNotFoundException.class)
                .verify();
    }

    private static SecurityUserRepository repositoryWithUser(SecurityUser user) {
        return new SecurityUserRepository() {
            @Override
            public Mono<ExternalIdentity> findIdentity(String issuer, String subject) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<SecurityUser> findByEmail(Email email) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<SecurityUser> findById(UserId userId) {
                return user != null && user.id().equals(userId) ? Mono.just(user) : Mono.empty();
            }

            @Override
            public Mono<String> providerFor(UserId userId) {
                return Mono.just("google");
            }

            @Override
            public Mono<SecurityUser> save(SecurityUser toSave) {
                return Mono.just(toSave);
            }

            @Override
            public Mono<Void> linkIdentity(ExternalIdentity identity) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Flux<SecurityUser> findAll() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
