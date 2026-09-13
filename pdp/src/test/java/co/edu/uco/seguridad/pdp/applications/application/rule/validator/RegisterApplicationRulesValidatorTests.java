package co.edu.uco.seguridad.pdp.applications.application.rule.validator;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.impl.RegisterApplicationRulesValidatorImpl;
import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.applications.domain.ApplicationCriteria;
import co.edu.uco.seguridad.pdp.applications.domain.exception.DuplicateApplicationException;
import co.edu.uco.seguridad.pdp.applications.domain.exception.ReservedApplicationNameException;
import co.edu.uco.seguridad.pdp.applications.domain.rule.impl.ApplicationNameMustBeUniqueForTenantRuleImpl;
import co.edu.uco.seguridad.pdp.applications.domain.rule.impl.ApplicationNameMustNotBeReservedRuleImpl;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Set;

/**
 * El orden importa y se afirma haciendo fallar dos reglas a la vez: con un nombre reservado el
 * validador no llega a consultar el repositorio, así que el error que sale es el barato.
 */
class RegisterApplicationRulesValidatorTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");

    @Test
    void completes_when_every_rule_passes() {
        StepVerifier.create(validator(false).execute(dto("gestion-academica"))).verifyComplete();
    }

    @Test
    void reports_a_duplicate_name_within_the_tenant() {
        StepVerifier.create(validator(true).execute(dto("gestion-academica")))
                .expectError(DuplicateApplicationException.class)
                .verify();
    }

    @Test
    void rejects_a_reserved_name_before_touching_the_repository() {
        StepVerifier.create(validator(true).execute(dto("admin")))
                .expectError(ReservedApplicationNameException.class)
                .verify();
    }

    private static RegisterApplicationRulesValidator validator(boolean nameTaken) {
        return new RegisterApplicationRulesValidatorImpl(
                new ApplicationNameMustNotBeReservedRuleImpl(Set.of("admin")),
                tenantId -> Mono.empty(),
                new ApplicationNameMustBeUniqueForTenantRuleImpl(),
                repositoryReporting(nameTaken));
    }

    private static RegisterApplicationRequest dto(String name) {
        return new RegisterApplicationRequest(TENANT, new ApplicationName(name), "",
                new ApplicationBaseUrl("https://gestion-academica.example.com"));
    }

    private static ApplicationRepository repositoryReporting(boolean exists) {
        return new ApplicationRepository() {
            @Override
            public Mono<Boolean> existsByTenantAndName(TenantId tenantId, ApplicationName name) {
                return Mono.just(exists);
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
