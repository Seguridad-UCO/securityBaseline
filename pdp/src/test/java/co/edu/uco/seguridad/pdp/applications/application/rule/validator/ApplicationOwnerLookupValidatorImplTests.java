package co.edu.uco.seguridad.pdp.applications.application.rule.validator;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.impl.ApplicationOwnerLookupValidatorImpl;
import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.domain.ApplicationCriteria;
import co.edu.uco.seguridad.pdp.applications.domain.exception.ApplicationNotFoundException;
import co.edu.uco.seguridad.pdp.commons.model.*;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

class ApplicationOwnerLookupValidatorImplTests {

    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final TenantId OWNER = new TenantId("universidad-uco");

    @Test
    void resolves_the_tenant_that_owns_an_existing_application() {
        ApplicationOwnerLookupValidatorImpl validator = new ApplicationOwnerLookupValidatorImpl(
                repositoryReturning(Mono.just(OWNER)));

        StepVerifier.create(validator.execute(APPLICATION))
                .expectNext(OWNER)
                .verifyComplete();
    }

    @Test
    void reports_application_not_found_when_no_tenant_owns_it() {
        ApplicationOwnerLookupValidatorImpl validator = new ApplicationOwnerLookupValidatorImpl(
                repositoryReturning(Mono.empty()));

        StepVerifier.create(validator.execute(APPLICATION))
                .expectError(ApplicationNotFoundException.class)
                .verify();
    }

    private static ApplicationRepository repositoryReturning(Mono<TenantId> result) {
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
                return result;
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
