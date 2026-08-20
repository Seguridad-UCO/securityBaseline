package co.edu.uco.seguridad.pdp.applications.application.rule;

import co.edu.uco.seguridad.pdp.applications.application.exception.DuplicateApplicationException;
import co.edu.uco.seguridad.pdp.applications.application.exception.ReservedApplicationNameException;
import co.edu.uco.seguridad.pdp.applications.application.port.primary.dto.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.pdp.applications.application.port.secondary.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.application.rule.impl.ApplicationNameMustBeUniqueForTenantRuleImpl;
import co.edu.uco.seguridad.pdp.applications.application.rule.impl.ApplicationNameMustNotBeReservedRuleImpl;
import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.domain.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cada regla de registro por sí sola. La regla de nombre reservado no necesita repositorio en absoluto, que es
 * por qué su prueba es una simple llamada a método sin Reactor a la vista.
 */
class ApplicationRegistrationRuleTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");

    @Test
    void reserved_name_rule_refuses_a_platform_name_whatever_its_case() {
        ApplicationNameMustNotBeReservedRule rule =
                new ApplicationNameMustNotBeReservedRuleImpl(Set.of("admin", "pdp"));

        ApplicationName reserved = new ApplicationName("Admin");
        assertThatThrownBy(() -> rule.execute(reserved))
                .isInstanceOf(ReservedApplicationNameException.class)
                .hasMessageContaining("reservado");
    }

    @Test
    void reserved_name_rule_allows_anything_else() {
        ApplicationNameMustNotBeReservedRule rule =
                new ApplicationNameMustNotBeReservedRuleImpl(Set.of("admin"));

        assertThatCode(() -> rule.execute(new ApplicationName("gestion-academica"))).doesNotThrowAnyException();
    }

    @Test
    void uniqueness_rule_completes_when_the_tenant_has_no_such_application() {
        ApplicationNameMustBeUniqueForTenantRule rule =
                new ApplicationNameMustBeUniqueForTenantRuleImpl(repositoryReporting(false));

        StepVerifier.create(rule.execute(dto("gestion-academica"))).verifyComplete();
    }

    @Test
    void uniqueness_rule_fails_when_the_tenant_already_registered_the_name() {
        ApplicationNameMustBeUniqueForTenantRule rule =
                new ApplicationNameMustBeUniqueForTenantRuleImpl(repositoryReporting(true));

        StepVerifier.create(rule.execute(dto("gestion-academica")))
                .expectError(DuplicateApplicationException.class)
                .verify();
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
            public Mono<Application> findByTenantAndId(TenantId tenantId, ApplicationId applicationId) {
                return Mono.empty();
            }

            @Override
            public Flux<Application> findAllByTenant(TenantId tenantId) {
                return Flux.empty();
            }

            @Override
            public Mono<Application> save(Application application) {
                return Mono.just(application);
            }

            @Override
            public Mono<Void> deleteById(ApplicationId applicationId) {
                return Mono.empty();
            }
        };
    }
}
