package co.edu.uco.seguridad.pdp.identity.application.usecase.impl;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.request.ProvisionIdentityRequest;
import co.edu.uco.seguridad.pdp.identity.application.secondaryport.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.identity.domain.SecurityUser;
import co.edu.uco.seguridad.pdp.identity.domain.model.Email;
import co.edu.uco.seguridad.pdp.identity.domain.model.ExternalIdentity;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProvisionIdentityUseCaseImplTests {

    private static final TenantId DEFAULT_TENANT = new TenantId("universidad-uco");
    private static final Instant NOW = Instant.parse("2026-08-20T00:00:00Z");

    @Test
    void a_known_identity_just_updates_the_login_timestamp_and_name() {
        SecurityUser existing = SecurityUser.provision(new UserId(UUID.randomUUID()), DEFAULT_TENANT,
                new Email("david@uco.edu"), "David", Instant.parse("2026-01-01T00:00:00Z"));
        FakeRepository repository = new FakeRepository();
        repository.users.put(existing.id(), existing);
        repository.identities.add(new ExternalIdentity(existing.id(), "issuer", "subject-1", "google"));

        ProvisionIdentityUseCaseImpl useCase = new ProvisionIdentityUseCaseImpl(repository, DEFAULT_TENANT,
                UUID::randomUUID, () -> NOW);

        StepVerifier.create(useCase.execute(new ProvisionIdentityRequest("issuer", "subject-1",
                        new Email("david@uco.edu"), "David Alzate", "google")))
                .assertNext(principal -> {
                    assertThat(principal.userId()).isEqualTo(existing.id().value().toString());
                    assertThat(principal.subject()).isEqualTo("subject-1");
                    assertThat(principal.name()).isEqualTo("David Alzate");
                })
                .verifyComplete();

        assertThat(repository.users.get(existing.id()).lastLoginAt()).isEqualTo(NOW);
        assertThat(repository.linked).isEmpty();
    }

    @Test
    void an_unknown_identity_with_a_known_email_links_instead_of_duplicating_the_user() {
        SecurityUser existing = SecurityUser.provision(new UserId(UUID.randomUUID()), DEFAULT_TENANT,
                new Email("david@uco.edu"), "David", Instant.parse("2026-01-01T00:00:00Z"));
        FakeRepository repository = new FakeRepository();
        repository.users.put(existing.id(), existing);

        ProvisionIdentityUseCaseImpl useCase = new ProvisionIdentityUseCaseImpl(repository, DEFAULT_TENANT,
                UUID::randomUUID, () -> NOW);

        StepVerifier.create(useCase.execute(new ProvisionIdentityRequest("issuer", "subject-2",
                        new Email("david@uco.edu"), "David", "keycloak-local")))
                .assertNext(principal -> assertThat(principal.userId()).isEqualTo(existing.id().value().toString()))
                .verifyComplete();

        assertThat(repository.linked).hasSize(1);
        assertThat(repository.linked.get(0).subject()).isEqualTo("subject-2");
        assertThat(repository.users).hasSize(1);
    }

    @Test
    void a_brand_new_identity_creates_a_user_in_the_default_tenant() {
        FakeRepository repository = new FakeRepository();
        UUID fixedId = UUID.randomUUID();

        ProvisionIdentityUseCaseImpl useCase = new ProvisionIdentityUseCaseImpl(repository, DEFAULT_TENANT,
                () -> fixedId, () -> NOW);

        StepVerifier.create(useCase.execute(new ProvisionIdentityRequest("issuer", "subject-3",
                        new Email("new-user@uco.edu"), "New User", "google")))
                .assertNext(principal -> {
                    assertThat(principal.userId()).isEqualTo(fixedId.toString());
                    assertThat(principal.tenantId()).isEqualTo(DEFAULT_TENANT);
                })
                .verifyComplete();

        assertThat(repository.users).hasSize(1);
        assertThat(repository.linked).hasSize(1);
    }

    private static final class FakeRepository implements SecurityUserRepository {

        private final java.util.Map<UserId, SecurityUser> users = new java.util.HashMap<>();
        private final List<ExternalIdentity> identities = new ArrayList<>();
        private final List<ExternalIdentity> linked = new ArrayList<>();

        @Override
        public Mono<ExternalIdentity> findIdentity(String issuer, String subject) {
            return identities.stream()
                    .filter(identity -> identity.issuer().equals(issuer) && identity.subject().equals(subject))
                    .findFirst()
                    .map(Mono::just)
                    .orElseGet(Mono::empty);
        }

        @Override
        public Mono<ExternalIdentity> findIdentityBySubject(String subject) {
            return identities.stream()
                    .filter(identity -> identity.subject().equals(subject))
                    .findFirst()
                    .map(Mono::just)
                    .orElseGet(Mono::empty);
        }

        @Override
        public Mono<SecurityUser> findByEmail(Email email) {
            return users.values().stream()
                    .filter(user -> user.email().equals(email))
                    .findFirst()
                    .map(Mono::just)
                    .orElseGet(Mono::empty);
        }

        @Override
        public Mono<SecurityUser> findById(UserId userId) {
            SecurityUser user = users.get(userId);
            return user == null ? Mono.empty() : Mono.just(user);
        }

        @Override
        public Mono<String> providerFor(UserId userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Mono<SecurityUser> save(SecurityUser user) {
            users.put(user.id(), user);
            return Mono.just(user);
        }

        @Override
        public Mono<Void> linkIdentity(ExternalIdentity identity) {
            linked.add(identity);
            identities.add(identity);
            return Mono.empty();
        }

        @Override
        public Flux<SecurityUser> findAll() {
            throw new UnsupportedOperationException();
        }
    }
}
