package co.edu.uco.seguridad.pdp.applications.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ValidateApplicationCredentialRequest;
import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.domain.ApplicationCriteria;
import co.edu.uco.seguridad.pdp.applications.domain.exception.InvalidApplicationCredentialException;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationCredentialHash;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationEnvironment;
import co.edu.uco.seguridad.pdp.applications.domain.rule.ApplicationCredentialMustBeValidRule;
import co.edu.uco.seguridad.pdp.applications.domain.rule.impl.ApplicationCredentialMustBeValidRuleImpl;
import co.edu.uco.seguridad.pdp.commons.model.*;
import co.edu.uco.seguridad.shared.port.CredentialHasher;
import co.edu.uco.seguridad.shared.security.Sha256CredentialHasher;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La regla real (no un dummy): esta prueba también demuestra que la regla y el validador encajan,
 * igual que el resto de casos de uso de una sola regla.
 */
class ValidateApplicationCredentialUseCaseImplTests {

    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final ApplicationName APPLICATION_NAME = new ApplicationName("notas");
    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final String CORRECT_SECRET = "secreto-correcto";
    private static final ApplicationCredentialHash HASH = new ApplicationCredentialHash("hash-guardado");

    @Test
    void a_correct_secret_for_an_existing_application_returns_its_tenant() {
        ApplicationCredentialMustBeValidRule rule = new ApplicationCredentialMustBeValidRuleImpl();
        ValidateApplicationCredentialUseCaseImpl useCase = new ValidateApplicationCredentialUseCaseImpl(
                rule, repository(Mono.just(HASH), Mono.just(TENANT)), hasher(true));

        StepVerifier.create(useCase.execute(new ValidateApplicationCredentialRequest(APPLICATION_NAME, CORRECT_SECRET)))
                .expectNext(TENANT)
                .verifyComplete();
    }

    @Test
    void an_incorrect_secret_is_rejected() {
        ApplicationCredentialMustBeValidRule rule = new ApplicationCredentialMustBeValidRuleImpl();
        ValidateApplicationCredentialUseCaseImpl useCase = new ValidateApplicationCredentialUseCaseImpl(
                rule, repository(Mono.just(HASH), Mono.just(TENANT)), hasher(false));

        StepVerifier.create(useCase.execute(new ValidateApplicationCredentialRequest(APPLICATION_NAME, "secreto-incorrecto")))
                .expectError(InvalidApplicationCredentialException.class)
                .verify();
    }

    @Test
    void a_correct_secret_is_rejected_when_the_environment_is_not_enabled() {
        ValidateApplicationCredentialUseCaseImpl useCase = new ValidateApplicationCredentialUseCaseImpl(
                new ApplicationCredentialMustBeValidRuleImpl(), repository(Mono.just(HASH), Mono.just(TENANT), Set.of(ApplicationEnvironment.PROD)), hasher(true));

        StepVerifier.create(useCase.execute(new ValidateApplicationCredentialRequest(APPLICATION_NAME, ApplicationEnvironment.LAB, CORRECT_SECRET)))
                .expectError(InvalidApplicationCredentialException.class)
                .verify();
    }

    @Test
    void an_application_that_does_not_exist_is_rejected_without_ever_comparing_a_secret() {
        ApplicationCredentialMustBeValidRule rule = new ApplicationCredentialMustBeValidRuleImpl();
        CredentialHasher poisonPill = new CredentialHasher() {
            @Override
            public String hash(String plaintext) {
                throw new AssertionError("must not hash");
            }

            @Override
            public boolean matches(String plaintext, String hash) {
                throw new AssertionError("must not compare a secret for an application that does not exist");
            }
        };
        ValidateApplicationCredentialUseCaseImpl useCase = new ValidateApplicationCredentialUseCaseImpl(
                rule, repository(Mono.empty(), Mono.empty()), poisonPill);

        StepVerifier.create(useCase.execute(new ValidateApplicationCredentialRequest(APPLICATION_NAME, CORRECT_SECRET)))
                .expectError(InvalidApplicationCredentialException.class)
                .verify();
    }

    @Test
    void a_correct_secret_stored_as_bcrypt_is_rewritten_as_sha256_and_keeps_validating() {
        Sha256CredentialHasher hasher = new Sha256CredentialHasher();
        ApplicationCredentialHash legacy = new ApplicationCredentialHash(new BCryptPasswordEncoder().encode(CORRECT_SECRET));
        AtomicReference<ApplicationCredentialHash> written = new AtomicReference<>();
        ValidateApplicationCredentialUseCaseImpl useCase = new ValidateApplicationCredentialUseCaseImpl(
                new ApplicationCredentialMustBeValidRuleImpl(),
                repository(Mono.just(legacy), Mono.just(TENANT), java.util.EnumSet.allOf(ApplicationEnvironment.class), written),
                hasher);

        StepVerifier.create(useCase.execute(new ValidateApplicationCredentialRequest(APPLICATION_NAME, CORRECT_SECRET)))
                .expectNext(TENANT)
                .verifyComplete();

        assertThat(written.get().value()).startsWith("sha256:");
        assertThat(hasher.matches(CORRECT_SECRET, written.get().value())).isTrue();
    }

    @Test
    void an_incorrect_secret_never_rewrites_a_legacy_hash() {
        ApplicationCredentialHash legacy = new ApplicationCredentialHash(new BCryptPasswordEncoder().encode(CORRECT_SECRET));
        AtomicReference<ApplicationCredentialHash> written = new AtomicReference<>();
        ValidateApplicationCredentialUseCaseImpl useCase = new ValidateApplicationCredentialUseCaseImpl(
                new ApplicationCredentialMustBeValidRuleImpl(),
                repository(Mono.just(legacy), Mono.just(TENANT), java.util.EnumSet.allOf(ApplicationEnvironment.class), written),
                new Sha256CredentialHasher());

        StepVerifier.create(useCase.execute(new ValidateApplicationCredentialRequest(APPLICATION_NAME, "secreto-incorrecto")))
                .expectError(InvalidApplicationCredentialException.class)
                .verify();

        assertThat(written.get()).isNull();
    }

    @Test
    void a_hash_already_in_sha256_is_not_rewritten() {
        Sha256CredentialHasher hasher = new Sha256CredentialHasher();
        ValidateApplicationCredentialUseCaseImpl useCase = new ValidateApplicationCredentialUseCaseImpl(
                new ApplicationCredentialMustBeValidRuleImpl(),
                repository(Mono.just(new ApplicationCredentialHash(hasher.hash(CORRECT_SECRET))), Mono.just(TENANT)),
                hasher);

        StepVerifier.create(useCase.execute(new ValidateApplicationCredentialRequest(APPLICATION_NAME, CORRECT_SECRET)))
                .expectNext(TENANT)
                .verifyComplete();
    }

    private static ApplicationRepository repository(Mono<ApplicationCredentialHash> hash, Mono<TenantId> tenantId) {
        return repository(hash, tenantId, java.util.EnumSet.allOf(ApplicationEnvironment.class));
    }

    private static ApplicationRepository repository(Mono<ApplicationCredentialHash> hash, Mono<TenantId> tenantId,
                                                    Set<ApplicationEnvironment> environments) {
        return repository(hash, tenantId, environments, null);
    }

    /** {@code written} recibe lo que el caso de uso reescriba; {@code null} = reescribir es un error de la prueba. */
    private static ApplicationRepository repository(Mono<ApplicationCredentialHash> hash, Mono<TenantId> tenantId,
                                                    Set<ApplicationEnvironment> environments,
                                                    AtomicReference<ApplicationCredentialHash> written) {
        return new ApplicationRepository() {
            @Override
            public Mono<Boolean> existsByTenantAndName(TenantId tenantId, ApplicationName name) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Boolean> existsByTenantAndId(TenantId tenantId, ApplicationId applicationId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<TenantId> findTenantIdById(ApplicationId applicationId) {
                return tenantId;
            }

            @Override
            public Mono<ApplicationCredentialHash> findCredentialHashById(ApplicationId applicationId) {
                return hash;
            }

            @Override
            public Mono<Application> findUniqueByName(ApplicationName applicationName) {
                return hash.map(value -> Application.register(APPLICATION, TENANT, APPLICATION_NAME, "",
                        new ApplicationBaseUrl("https://notas.example.test"), value, java.time.Instant.EPOCH, environments));
            }

            @Override
            public Mono<Application> findByIdForTenant(TenantId tenantId, ApplicationId applicationId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Void> updateCredentialHash(ApplicationId applicationId,
                                                   ApplicationCredentialHash credentialHash) {
                if (written == null) {
                    throw new UnsupportedOperationException();
                }
                return Mono.fromRunnable(() -> written.set(credentialHash));
            }

            @Override
            public Mono<ResultPage<Application>> findBy(ApplicationCriteria criteria, PageWindow window) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Application> save(Application application) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Void> deleteById(ApplicationId applicationId) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static CredentialHasher hasher(boolean matches) {
        return new CredentialHasher() {
            @Override
            public String hash(String plaintext) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean matches(String plaintext, String hash) {
                return matches;
            }
        };
    }
}
