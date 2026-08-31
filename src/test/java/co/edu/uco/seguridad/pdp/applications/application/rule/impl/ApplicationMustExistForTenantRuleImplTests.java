package co.edu.uco.seguridad.pdp.applications.application.rule.impl;

import co.edu.uco.seguridad.pdp.applications.application.exception.ApplicationNotFoundException;
import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ApplicationOwnershipQuery;
import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.domain.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.applications.domain.ApplicationCriteria;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.PageWindow;
import co.edu.uco.seguridad.pdp.commons.ResultPage;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

class ApplicationMustExistForTenantRuleImplTests {

    private static final TenantId UCO = new TenantId("universidad-uco");
    private static final ApplicationId ID = new ApplicationId(UUID.randomUUID());

    @Test
    void hands_back_the_application_when_it_belongs_to_the_tenant() {
        Application application = application();
        ApplicationMustExistForTenantRuleImpl rule =
                new ApplicationMustExistForTenantRuleImpl(repositoryReturning(application));

        StepVerifier.create(rule.execute(new ApplicationOwnershipQuery(UCO, ID)))
                .expectNext(application)
                .verifyComplete();
    }

    @Test
    void refuses_an_application_that_the_tenant_does_not_own() {
        ApplicationMustExistForTenantRuleImpl rule =
                new ApplicationMustExistForTenantRuleImpl(repositoryReturning(null));

        StepVerifier.create(rule.execute(new ApplicationOwnershipQuery(UCO, ID)))
                .expectError(ApplicationNotFoundException.class)
                .verify();
    }

    private static Application application() {
        return new Application(ID, UCO, new ApplicationName("gestion-academica"), "",
                new ApplicationBaseUrl("https://example.com"), Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static ApplicationRepository repositoryReturning(Application application) {
        return new ApplicationRepository() {
            @Override
            public Mono<Boolean> existsByTenantAndName(TenantId tenantId, ApplicationName name) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Application> findByTenantAndId(TenantId tenantId, ApplicationId applicationId) {
                return application == null ? Mono.empty() : Mono.just(application);
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
