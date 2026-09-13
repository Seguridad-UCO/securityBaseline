package co.edu.uco.seguridad.pdp.applications.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.RotateApplicationCredentialRequest;
import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.ApplicationRegistrationResponse;
import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.domain.ApplicationCriteria;
import co.edu.uco.seguridad.pdp.applications.domain.exception.ApplicationNotFoundException;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationCredentialHash;
import co.edu.uco.seguridad.pdp.applications.domain.rule.ApplicationMustExistForTenantRule;
import co.edu.uco.seguridad.pdp.applications.domain.rule.impl.ApplicationMustExistForTenantRuleImpl;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.port.CredentialHasher;
import co.edu.uco.seguridad.shared.port.SecretGenerator;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La regla real (no un dummy): con una sola regla y ningún otro consumidor, el propio caso de uso
 * hace de validador — igual que el resto de casos de uso de una sola regla del slice.
 */
class RotateApplicationCredentialUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final Instant REGISTERED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final String NEW_SECRET = "secreto-nuevo-en-claro";
    private static final String NEW_HASH = "hash-del-secreto-nuevo";

    @Test
    void rotates_an_existing_application_returning_the_new_secret_and_persisting_only_its_hash() {
        Application existing = Application.register(APPLICATION, TENANT, new ApplicationName("gestion-academica"),
                "Sistema académico", new ApplicationBaseUrl("https://example.com"),
                new ApplicationCredentialHash("hash-viejo"), REGISTERED_AT);
        List<ApplicationCredentialHash> updated = new ArrayList<>();
        RotateApplicationCredentialUseCaseImpl useCase = new RotateApplicationCredentialUseCaseImpl(
                repository(Mono.just(existing), updated), rule(), () -> NEW_SECRET, hasher(NEW_HASH));

        StepVerifier.create(useCase.execute(new RotateApplicationCredentialRequest(TENANT, APPLICATION)))
                .assertNext(response -> {
                    assertThat(response.credential()).isEqualTo(NEW_SECRET);
                    assertThat(response.application().id()).isEqualTo(APPLICATION);
                    assertThat(response.application().tenantId()).isEqualTo(TENANT);
                    assertThat(response.application().name()).isEqualTo(new ApplicationName("gestion-academica"));
                    assertThat(response.application().baseUrl()).isEqualTo(new ApplicationBaseUrl("https://example.com"));
                    assertThat(response.application().registeredAt()).isEqualTo(REGISTERED_AT);
                })
                .verifyComplete();

        assertThat(updated).containsExactly(new ApplicationCredentialHash(NEW_HASH));
    }

    @Test
    void an_application_that_does_not_exist_is_rejected_without_ever_generating_or_hashing_a_secret() {
        List<ApplicationCredentialHash> updated = new ArrayList<>();
        SecretGenerator poisonGenerator = () -> {
            throw new AssertionError("must not generate a secret for an application that does not exist");
        };
        CredentialHasher poisonHasher = new CredentialHasher() {
            @Override
            public String hash(String plaintext) {
                throw new AssertionError("must not hash for an application that does not exist");
            }

            @Override
            public boolean matches(String plaintext, String hash) {
                throw new AssertionError("must not compare a secret for an application that does not exist");
            }
        };
        RotateApplicationCredentialUseCaseImpl useCase = new RotateApplicationCredentialUseCaseImpl(
                repository(Mono.empty(), updated), rule(), poisonGenerator, poisonHasher);

        StepVerifier.create(useCase.execute(new RotateApplicationCredentialRequest(TENANT, APPLICATION)))
                .expectError(ApplicationNotFoundException.class)
                .verify();

        assertThat(updated).isEmpty();
    }

    private static ApplicationMustExistForTenantRule rule() {
        return new ApplicationMustExistForTenantRuleImpl();
    }

    private static CredentialHasher hasher(String hashed) {
        return new CredentialHasher() {
            @Override
            public String hash(String plaintext) {
                return hashed;
            }

            @Override
            public boolean matches(String plaintext, String hash) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static ApplicationRepository repository(Mono<Application> found, List<ApplicationCredentialHash> updated) {
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
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ApplicationCredentialHash> findCredentialHashById(ApplicationId applicationId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Application> findByIdForTenant(TenantId tenantId, ApplicationId applicationId) {
                return found;
            }

            @Override
            public Mono<Void> updateCredentialHash(ApplicationId applicationId, ApplicationCredentialHash credentialHash) {
                updated.add(credentialHash);
                return Mono.empty();
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
}
