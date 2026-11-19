package co.edu.uco.seguridad.pdp.applications.application.rule.validator;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ApplicationOwnershipQuery;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.impl.ApplicationMustExistForTenantValidatorImpl;
import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.domain.ApplicationCriteria;
import co.edu.uco.seguridad.pdp.applications.domain.exception.ApplicationNotFoundException;
import co.edu.uco.seguridad.pdp.applications.domain.rule.impl.ApplicationMustExistForTenantRuleImpl;
import co.edu.uco.seguridad.pdp.commons.model.*;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

class ApplicationMustExistForTenantValidatorTests {

    private static final TenantId UCO = new TenantId("universidad-uco");
    private static final ApplicationId ID = new ApplicationId(UUID.randomUUID());

    @Test
    void completes_when_the_application_belongs_to_the_tenant() {
        StepVerifier.create(validator(true).execute(new ApplicationOwnershipQuery(UCO, ID))).verifyComplete();
    }

    @Test
    void refuses_an_application_that_the_tenant_does_not_own() {
        StepVerifier.create(validator(false).execute(new ApplicationOwnershipQuery(UCO, ID)))
                .expectError(ApplicationNotFoundException.class)
                .verify();
    }

    private static ApplicationMustExistForTenantValidator validator(boolean registered) {
        return new ApplicationMustExistForTenantValidatorImpl(repositoryReporting(registered),
                new ApplicationMustExistForTenantRuleImpl());
    }

    private static ApplicationRepository repositoryReporting(boolean registered) {
        return new ApplicationRepository() {
            @Override
            public Mono<Boolean> existsByTenantAndName(TenantId tenantId, ApplicationName name) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Boolean> existsByTenantAndId(TenantId tenantId, ApplicationId applicationId) {
                return Mono.just(registered);
            }

            @Override
            public Mono<TenantId> findTenantIdById(ApplicationId applicationId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationCredentialHash> findCredentialHashById(
                    ApplicationId applicationId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Application> findByIdForTenant(TenantId tenantId, ApplicationId applicationId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Void> updateCredentialHash(ApplicationId applicationId,
                                                   co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationCredentialHash credentialHash) {
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
}
