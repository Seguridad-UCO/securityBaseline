package co.edu.uco.seguridad.pdp.applications.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.RegisterApplicationRulesValidator;
import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.domain.ApplicationCriteria;
import co.edu.uco.seguridad.pdp.applications.domain.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.PageWindow;
import co.edu.uco.seguridad.pdp.commons.ResultPage;
import co.edu.uco.seguridad.pdp.commons.TenantId;
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
 * es la construcción de la entidad y la persistencia, no las reglas (esas ya se prueban solas).
 */
class RegisterApplicationUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final Instant NOW = Instant.parse("2026-08-20T00:00:00Z");

    @Test
    void registers_the_application_with_a_generated_id_and_the_current_time() {
        List<Application> saved = new ArrayList<>();
        ApplicationRepository repository = new ApplicationRepository() {
            @Override
            public Mono<Boolean> existsByTenantAndName(TenantId tenantId, ApplicationName name) {
                return Mono.just(false);
            }

            @Override
            public Mono<Application> findByTenantAndId(TenantId tenantId, ApplicationId applicationId) {
                return Mono.empty();
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
                dto -> Mono.empty(), repository, () -> fixedId, () -> NOW);

        RegisterApplicationRequest request = new RegisterApplicationRequest(TENANT,
                new ApplicationName("gestion-academica"), "Sistema académico",
                new ApplicationBaseUrl("https://gestion-academica.uco.edu.co"));

        StepVerifier.create(useCase.execute(request))
                .assertNext(response -> {
                    assertThat(response.id()).isEqualTo(new ApplicationId(fixedId));
                    assertThat(response.tenantId()).isEqualTo(TENANT);
                    assertThat(response.name()).isEqualTo(new ApplicationName("gestion-academica"));
                    assertThat(response.description()).isEqualTo("Sistema académico");
                    assertThat(response.registeredAt()).isEqualTo(NOW);
                })
                .verifyComplete();

        assertThat(saved).hasSize(1);
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
            public Mono<Application> findByTenantAndId(TenantId tenantId, ApplicationId applicationId) {
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
                alwaysRejects, repository, UUID::randomUUID, () -> NOW);

        RegisterApplicationRequest request = new RegisterApplicationRequest(TENANT,
                new ApplicationName("admin"), "", new ApplicationBaseUrl("https://admin.example.com"));

        StepVerifier.create(useCase.execute(request)).expectErrorMessage("nombre reservado").verify();
    }
}
