package co.edu.uco.seguridad.pdp.applications.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.RegisterApplicationRulesValidator;
import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.domain.ApplicationCriteria;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El caso de uso con las reglas sustituidas por un dummy que siempre aprueba: lo que se prueba aquí
 * es la construcción de la entidad, la persistencia y —desde HU-012— que el secreto en claro nunca
 * es lo que se persiste.
 */
class RegisterApplicationUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final Instant NOW = Instant.parse("2026-08-20T00:00:00Z");
    private static final String PLAINTEXT_SECRET = "secreto-en-claro";
    private static final String HASHED_SECRET = "hash-del-secreto";

    @Test
    void registers_the_application_with_a_generated_id_and_the_current_time() {
        List<Application> saved = new ArrayList<>();
        ApplicationRepository repository = new ApplicationRepository() {
            @Override
            public Mono<Boolean> existsByTenantAndName(TenantId tenantId, ApplicationName name) {
                return Mono.just(false);
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
            public Mono<ResultPage<Application>> findBy(ApplicationCriteria criteria, PageWindow window) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Application> save(Application application) {
                saved.add(application);
                return Mono.just(application);
            }

            @Override
            public Mono<Void> deleteById(ApplicationId applicationId) {
                return Mono.empty();
            }
        };
        UUID fixedId = UUID.randomUUID();
        RegisterApplicationUseCaseImpl useCase = new RegisterApplicationUseCaseImpl(
                dto -> Mono.empty(), repository, () -> fixedId, () -> NOW,
                () -> PLAINTEXT_SECRET, plaintext -> HASHED_SECRET);

        RegisterApplicationRequest request = new RegisterApplicationRequest(TENANT,
                new ApplicationName("gestion-academica"), "Sistema académico",
                new ApplicationBaseUrl("https://gestion-academica.uco.edu.co"));

        StepVerifier.create(useCase.execute(request))
                .assertNext(response -> {
                    assertThat(response.application().id()).isEqualTo(new ApplicationId(fixedId));
                    assertThat(response.application().tenantId()).isEqualTo(TENANT);
                    assertThat(response.application().name()).isEqualTo(new ApplicationName("gestion-academica"));
                    assertThat(response.application().description()).isEqualTo("Sistema académico");
                    assertThat(response.application().registeredAt()).isEqualTo(NOW);
                    assertThat(response.credential()).isEqualTo(PLAINTEXT_SECRET);
                })
                .verifyComplete();

        assertThat(saved).hasSize(1);
    }

    @Test
    void never_persists_the_plaintext_secret_only_its_hash() {
        List<Application> saved = new ArrayList<>();
        ApplicationRepository repository = new ApplicationRepository() {
            @Override
            public Mono<Boolean> existsByTenantAndName(TenantId tenantId, ApplicationName name) {
                return Mono.just(false);
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
            public Mono<ResultPage<Application>> findBy(ApplicationCriteria criteria, PageWindow window) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Application> save(Application application) {
                saved.add(application);
                return Mono.just(application);
            }

            @Override
            public Mono<Void> deleteById(ApplicationId applicationId) {
                throw new UnsupportedOperationException();
            }
        };
        RegisterApplicationUseCaseImpl useCase = new RegisterApplicationUseCaseImpl(
                dto -> Mono.empty(), repository, UUID::randomUUID, () -> NOW,
                () -> PLAINTEXT_SECRET, plaintext -> HASHED_SECRET);

        RegisterApplicationRequest request = new RegisterApplicationRequest(TENANT,
                new ApplicationName("gestion-academica"), "", new ApplicationBaseUrl("https://example.com"));

        StepVerifier.create(useCase.execute(request))
                .assertNext(response -> assertThat(response.credential()).isEqualTo(PLAINTEXT_SECRET))
                .verifyComplete();

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).credentialHash().value())
                .isEqualTo(HASHED_SECRET)
                .isNotEqualTo(PLAINTEXT_SECRET);
    }

    @Test
    void never_saves_when_the_rules_validator_rejects_the_request() {
        RuntimeException rejection = new RuntimeException("nombre reservado");
        ApplicationRepository repository = new ApplicationRepository() {
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
            public Mono<ResultPage<Application>> findBy(ApplicationCriteria criteria, PageWindow window) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Application> save(Application application) {
                throw new AssertionError("must not save when the rules reject the request");
            }

            @Override
            public Mono<Void> deleteById(ApplicationId applicationId) {
                throw new UnsupportedOperationException();
            }
        };
        RegisterApplicationRulesValidator alwaysRejects = dto -> Mono.error(rejection);
        RegisterApplicationUseCaseImpl useCase = new RegisterApplicationUseCaseImpl(
                alwaysRejects, repository, UUID::randomUUID, () -> NOW,
                () -> { throw new AssertionError("must not generate a secret when the rules reject the request"); },
                plaintext -> { throw new AssertionError("must not hash when the rules reject the request"); });

        RegisterApplicationRequest request = new RegisterApplicationRequest(TENANT,
                new ApplicationName("admin"), "", new ApplicationBaseUrl("https://admin.example.com"));

        StepVerifier.create(useCase.execute(request)).expectErrorMessage("nombre reservado").verify();
    }
}
