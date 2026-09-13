package co.edu.uco.seguridad.pdp.applications.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ValidateApplicationCredentialRequest;
import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.domain.ApplicationCriteria;
import co.edu.uco.seguridad.pdp.applications.domain.exception.InvalidApplicationCredentialException;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationCredentialHash;
import co.edu.uco.seguridad.pdp.applications.domain.rule.ApplicationCredentialMustBeValidRule;
import co.edu.uco.seguridad.pdp.applications.domain.rule.impl.ApplicationCredentialMustBeValidRuleImpl;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.port.CredentialHasher;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

/**
 * La regla real (no un dummy): esta prueba también demuestra que la regla y el validador encajan,
 * igual que el resto de casos de uso de una sola regla.
 */
class ValidateApplicationCredentialUseCaseImplTests {

    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final String CORRECT_SECRET = "secreto-correcto";
    private static final ApplicationCredentialHash HASH = new ApplicationCredentialHash("hash-guardado");

    @Test
    void a_correct_secret_for_an_existing_application_returns_its_tenant() {
        ApplicationCredentialMustBeValidRule rule = new ApplicationCredentialMustBeValidRuleImpl();
        ValidateApplicationCredentialUseCaseImpl useCase = new ValidateApplicationCredentialUseCaseImpl(
                rule, repository(Mono.just(HASH), Mono.just(TENANT)), hasher(true));

        StepVerifier.create(useCase.execute(new ValidateApplicationCredentialRequest(APPLICATION, CORRECT_SECRET)))
                .expectNext(TENANT)
                .verifyComplete();
    }

    @Test
    void an_incorrect_secret_is_rejected() {
        ApplicationCredentialMustBeValidRule rule = new ApplicationCredentialMustBeValidRuleImpl();
        ValidateApplicationCredentialUseCaseImpl useCase = new ValidateApplicationCredentialUseCaseImpl(
                rule, repository(Mono.just(HASH), Mono.just(TENANT)), hasher(false));

        StepVerifier.create(useCase.execute(new ValidateApplicationCredentialRequest(APPLICATION, "secreto-incorrecto")))
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

        StepVerifier.create(useCase.execute(new ValidateApplicationCredentialRequest(APPLICATION, CORRECT_SECRET)))
                .expectError(InvalidApplicationCredentialException.class)
                .verify();
    }

    private static ApplicationRepository repository(Mono<ApplicationCredentialHash> hash, Mono<TenantId> tenantId) {
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
            public Mono<Application> findByIdForTenant(TenantId tenantId, ApplicationId applicationId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Void> updateCredentialHash(ApplicationId applicationId,
                    ApplicationCredentialHash credentialHash) {
                throw new UnsupportedOperationException();
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
