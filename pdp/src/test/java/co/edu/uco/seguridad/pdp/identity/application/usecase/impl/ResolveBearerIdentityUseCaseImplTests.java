package co.edu.uco.seguridad.pdp.identity.application.usecase.impl;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.request.ResolveBearerIdentityRequest;
import co.edu.uco.seguridad.pdp.identity.application.secondaryport.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.identity.domain.SecurityUser;
import co.edu.uco.seguridad.pdp.identity.domain.exception.InvalidEmailException;
import co.edu.uco.seguridad.pdp.identity.domain.exception.UserEmailAlreadyRegisteredException;
import co.edu.uco.seguridad.pdp.identity.domain.model.Email;
import co.edu.uco.seguridad.pdp.identity.domain.model.ExternalIdentity;
import co.edu.uco.seguridad.pdp.identity.domain.rule.impl.UserEmailMustBeAvailableRuleImpl;
import co.edu.uco.seguridad.pdp.tenants.application.rule.validator.TenantMustBeActiveValidator;
import co.edu.uco.seguridad.pdp.tenants.domain.exception.TenantNotActiveException;
import co.edu.uco.seguridad.pdp.tenants.domain.exception.TenantNotFoundException;
import co.edu.uco.seguridad.pdp.tenants.domain.model.TenantStatus;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ResolveBearerIdentityUseCaseImplTests {

    private static final String ISSUER = "https://idp.test/realms/uco";
    private static final String SUBJECT = "kc-subject-1";
    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final Instant NOW = Instant.parse("2026-10-30T10:00:00Z");
    private static final UUID GENERATED = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UserId EXISTING_USER = new UserId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final UserId WINNER_USER = new UserId(UUID.fromString("33333333-3333-3333-3333-333333333333"));

    @Test
    void returns_the_user_id_of_an_existing_identity_without_writing_anything() {
        FakeRepository repository = new FakeRepository();
        repository.identities.add(new ExternalIdentity(EXISTING_USER, ISSUER, SUBJECT, "keycloak-local"));
        List<TenantId> validated = new ArrayList<>();

        StepVerifier.create(useCase(repository, tenant -> {
                    validated.add(tenant);
                    return Mono.empty();
                }).execute(request(Optional.of(new Email("david@uco.edu.co")), true)))
                .expectNext(EXISTING_USER)
                .verifyComplete();

        assertThat(repository.created).isEmpty();
        assertThat(validated).isEmpty();
    }

    @Test
    void creates_user_and_link_in_the_tenant_of_the_claim_for_an_unseen_subject() {
        FakeRepository repository = new FakeRepository();

        StepVerifier.create(useCase(repository, tenant -> Mono.empty())
                        .execute(request(Optional.of(new Email("David@uco.edu.co")), true)))
                .expectNext(new UserId(GENERATED))
                .verifyComplete();

        assertThat(repository.created).hasSize(1);
        SecurityUser user = repository.created.get(0).user();
        ExternalIdentity identity = repository.created.get(0).identity();
        assertThat(user.id()).isEqualTo(new UserId(GENERATED));
        assertThat(user.tenantId()).isEqualTo(TENANT);
        assertThat(user.email()).isEqualTo(new Email("david@uco.edu.co"));
        assertThat(user.name()).isEqualTo("David");
        assertThat(user.createdAt()).isEqualTo(NOW);
        assertThat(identity).isEqualTo(new ExternalIdentity(new UserId(GENERATED), ISSUER, SUBJECT, "keycloak-local"));
    }

    @Test
    void refuses_an_unknown_tenant_and_creates_nothing() {
        FakeRepository repository = new FakeRepository();

        StepVerifier.create(useCase(repository, tenant -> Mono.error(new TenantNotFoundException(tenant)))
                        .execute(request(Optional.of(new Email("david@uco.edu.co")), true)))
                .expectError(TenantNotFoundException.class)
                .verify();

        assertThat(repository.created).isEmpty();
    }

    @Test
    void refuses_a_suspended_tenant_and_creates_nothing() {
        FakeRepository repository = new FakeRepository();

        StepVerifier.create(useCase(repository,
                        tenant -> Mono.error(new TenantNotActiveException(tenant, TenantStatus.SUSPENDED)))
                        .execute(request(Optional.of(new Email("david@uco.edu.co")), true)))
                .expectError(TenantNotActiveException.class)
                .verify();

        assertThat(repository.created).isEmpty();
    }

    @Test
    void refuses_an_email_that_already_belongs_to_another_user_and_creates_nothing() {
        FakeRepository repository = new FakeRepository();
        Email taken = new Email("david@uco.edu.co");
        repository.users.add(SecurityUser.provision(EXISTING_USER, TENANT, taken, "Otro", NOW));

        StepVerifier.create(useCase(repository, tenant -> Mono.empty()).execute(request(Optional.of(taken), true)))
                .expectError(UserEmailAlreadyRegisteredException.class)
                .verify();

        assertThat(repository.created).isEmpty();
    }

    @Test
    void uses_the_reserved_placeholder_email_when_the_token_has_no_email() {
        FakeRepository repository = new FakeRepository();

        StepVerifier.create(useCase(repository, tenant -> Mono.empty()).execute(request(Optional.empty(), false)))
                .expectNext(new UserId(GENERATED))
                .verifyComplete();

        assertThat(repository.created.get(0).user().email()).isEqualTo(new Email(SUBJECT + "@bearer.invalid"));
    }

    @Test
    void uses_the_placeholder_email_when_the_token_email_is_not_verified() {
        FakeRepository repository = new FakeRepository();
        repository.users.add(SecurityUser.provision(EXISTING_USER, TENANT, new Email("david@uco.edu.co"), "Otro", NOW));

        StepVerifier.create(useCase(repository, tenant -> Mono.empty())
                        .execute(request(Optional.of(new Email("david@uco.edu.co")), false)))
                .expectNext(new UserId(GENERATED))
                .verifyComplete();

        assertThat(repository.created.get(0).user().email()).isEqualTo(new Email(SUBJECT + "@bearer.invalid"));
    }

    @Test
    void fails_when_the_subject_cannot_form_a_valid_placeholder_email() {
        FakeRepository repository = new FakeRepository();
        ResolveBearerIdentityRequest request = new ResolveBearerIdentityRequest(ISSUER, "sujeto con espacios", TENANT,
                Optional.empty(), false, "David", "keycloak-local");

        StepVerifier.create(useCase(repository, tenant -> Mono.empty()).execute(request))
                .expectError(InvalidEmailException.class)
                .verify();

        assertThat(repository.created).isEmpty();
    }

    @Test
    void returns_the_winning_user_id_when_a_concurrent_creation_wins_the_race() {
        FakeRepository repository = new FakeRepository();
        repository.raceWinner = new ExternalIdentity(WINNER_USER, ISSUER, SUBJECT, "keycloak-local");

        StepVerifier.create(useCase(repository, tenant -> Mono.empty())
                        .execute(request(Optional.of(new Email("david@uco.edu.co")), true)))
                .expectNext(WINNER_USER)
                .verifyComplete();
    }

    @Test
    void propagates_a_persistence_failure() {
        FakeRepository repository = new FakeRepository();
        repository.failure = new IllegalStateException("surreal caido");

        StepVerifier.create(useCase(repository, tenant -> Mono.empty())
                        .execute(request(Optional.of(new Email("david@uco.edu.co")), true)))
                .expectError(IllegalStateException.class)
                .verify();
    }

    private static ResolveBearerIdentityUseCaseImpl useCase(FakeRepository repository, TenantMustBeActiveValidator validator) {
        return new ResolveBearerIdentityUseCaseImpl(repository, validator, new UserEmailMustBeAvailableRuleImpl(),
                () -> GENERATED, () -> NOW);
    }

    private static ResolveBearerIdentityRequest request(Optional<Email> email, boolean verified) {
        return new ResolveBearerIdentityRequest(ISSUER, SUBJECT, TENANT, email, verified, "David", "keycloak-local");
    }

    private record Created(SecurityUser user, ExternalIdentity identity) {
    }

    private static final class FakeRepository implements SecurityUserRepository {

        private final List<ExternalIdentity> identities = new ArrayList<>();
        private final List<SecurityUser> users = new ArrayList<>();
        private final List<Created> created = new ArrayList<>();
        private ExternalIdentity raceWinner;
        private RuntimeException failure;

        @Override
        public Mono<ExternalIdentity> findIdentity(String issuer, String subject) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Mono<ExternalIdentity> findIdentityBySubject(String subject) {
            return identities.stream().filter(identity -> identity.subject().equals(subject)).findFirst()
                    .map(Mono::just).orElseGet(Mono::empty);
        }

        @Override
        public Mono<SecurityUser> findByEmail(Email email) {
            return users.stream().filter(user -> user.email().equals(email)).findFirst()
                    .map(Mono::just).orElseGet(Mono::empty);
        }

        @Override
        public Mono<SecurityUser> findById(UserId userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Mono<String> providerFor(UserId userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Mono<SecurityUser> save(SecurityUser user) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Mono<Void> linkIdentity(ExternalIdentity identity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Flux<SecurityUser> findAll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Mono<ExternalIdentity> createUserWithIdentityIfAbsent(SecurityUser user, ExternalIdentity identity) {
            if (failure != null) return Mono.error(failure);
            if (raceWinner != null) return Mono.just(raceWinner);
            created.add(new Created(user, identity));
            return Mono.just(identity);
        }
    }
}
