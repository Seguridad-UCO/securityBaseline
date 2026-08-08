package co.edu.uco.seguridad.pdp.recursos.application.rule;

import co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.dto.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.PageWindow;
import co.edu.uco.seguridad.pdp.commons.ResourceId;
import co.edu.uco.seguridad.pdp.commons.ResultPage;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.recursos.application.exception.DuplicateProtectedResourceException;
import co.edu.uco.seguridad.pdp.recursos.application.exception.ResourceTenantMismatchException;
import co.edu.uco.seguridad.pdp.recursos.application.model.ProtectedResourceRegistration;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.request.RegisterProtectedApplicationRequest;
import co.edu.uco.seguridad.pdp.recursos.application.port.secondary.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.recursos.application.rule.impl.ProtectedResourceMustBeUniqueRuleImpl;
import co.edu.uco.seguridad.pdp.recursos.application.rule.impl.ProtectedResourceMustBelongToApplicationTenantRuleImpl;
import co.edu.uco.seguridad.pdp.recursos.domain.ActionCode;
import co.edu.uco.seguridad.pdp.recursos.domain.ProtectedApplicationCriteria;
import co.edu.uco.seguridad.pdp.recursos.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.recursos.domain.ResourceCode;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProtectedResourceRuleTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());

    @Test
    void tenant_consistency_rule_passes_when_both_tenants_agree() {
        ProtectedResourceMustBelongToApplicationTenantRule rule =
                new ProtectedResourceMustBelongToApplicationTenantRuleImpl();

        assertThatCode(() -> rule.execute(registration(TENANT, TENANT))).doesNotThrowAnyException();
    }

    @Test
    void tenant_consistency_rule_fails_when_the_application_belongs_to_another_tenant() {
        ProtectedResourceMustBelongToApplicationTenantRule rule =
                new ProtectedResourceMustBelongToApplicationTenantRuleImpl();

        assertThatThrownBy(() -> rule.execute(registration(TENANT, new TenantId("otra-universidad"))))
                .isInstanceOf(ResourceTenantMismatchException.class);
    }

    @Test
    void uniqueness_rule_completes_when_the_grant_does_not_exist_yet() {
        ProtectedResourceMustBeUniqueRule rule =
                new ProtectedResourceMustBeUniqueRuleImpl(repositoryReporting(false));

        StepVerifier.create(rule.execute(registration(TENANT, TENANT))).verifyComplete();
    }

    @Test
    void uniqueness_rule_fails_when_the_same_grant_is_registered_twice() {
        ProtectedResourceMustBeUniqueRule rule =
                new ProtectedResourceMustBeUniqueRuleImpl(repositoryReporting(true));

        StepVerifier.create(rule.execute(registration(TENANT, TENANT)))
                .expectError(DuplicateProtectedResourceException.class)
                .verify();
    }

    private static ProtectedResourceRegistration registration(TenantId requested, TenantId owning) {
        RegisterProtectedApplicationRequest dto = new RegisterProtectedApplicationRequest(
                requested,
                new ApplicationName("gestion-academica"),
                new ResourceCode("estudiantes"),
                new ActionCode("consultar"));
        RegisteredApplicationResponse application = new RegisteredApplicationResponse(
                APPLICATION, owning, new ApplicationName("gestion-academica"), Instant.parse("2026-08-07T12:00:00Z"));
        return new ProtectedResourceRegistration(dto, application);
    }

    private static ProtectedResourceRepository repositoryReporting(boolean exists) {
        return new ProtectedResourceRepository() {
            @Override
            public Mono<Boolean> existsGrant(ApplicationId applicationId, ResourceCode code, ActionCode action) {
                return Mono.just(exists);
            }

            @Override
            public Mono<ResultPage<ProtectedResource>> findBy(ProtectedApplicationCriteria criteria,
                                                              PageWindow window) {
                return Mono.just(ResultPage.of(List.of(), 0, window));
            }

            @Override
            public Mono<ProtectedResource> save(ProtectedResource resource) {
                return Mono.just(resource);
            }

            @Override
            public Mono<Void> deleteById(ResourceId resourceId) {
                return Mono.empty();
            }
        };
    }
}
