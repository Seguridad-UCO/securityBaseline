package co.edu.uco.seguridad.pdp.tenants.application.rule;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.tenants.TenantStatus;
import co.edu.uco.seguridad.pdp.tenants.application.exception.TenantNotActiveException;
import co.edu.uco.seguridad.pdp.tenants.application.exception.TenantNotFoundException;
import co.edu.uco.seguridad.pdp.tenants.application.port.secondary.repository.TenantRepository;
import co.edu.uco.seguridad.pdp.tenants.application.rule.impl.TenantMustBeActiveRuleImpl;
import co.edu.uco.seguridad.pdp.tenants.domain.Tenant;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La regla aislada, contra un stub de repositorio escrito a mano. Los dos modos de fallo se
 * afirman por separado porque colapserlos era exactamente lo que el divide de
 * {@code TenantUnavailableException} estaba destinado a arreglar.
 */
class TenantMustBeActiveRuleImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");

    @Test
    void passes_and_returns_the_snapshot_for_an_active_tenant() {
        TenantMustBeActiveRuleImpl rule = ruleFor(new Tenant(TENANT, TenantStatus.ACTIVE));

        StepVerifier.create(rule.execute(TENANT))
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(TENANT);
                    assertThat(dto.active()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void reports_an_unknown_tenant_as_not_found() {
        TenantMustBeActiveRuleImpl rule = new TenantMustBeActiveRuleImpl(id -> Mono.empty());

        StepVerifier.create(rule.execute(TENANT))
                .expectError(TenantNotFoundException.class)
                .verify();
    }

    @Test
    void reports_a_suspended_tenant_as_not_active() {
        TenantMustBeActiveRuleImpl rule = ruleFor(new Tenant(TENANT, TenantStatus.SUSPENDED));

        StepVerifier.create(rule.execute(TENANT))
                .expectError(TenantNotActiveException.class)
                .verify();
    }

    private static TenantMustBeActiveRuleImpl ruleFor(Tenant tenant) {
        TenantRepository repository = id -> id.equals(tenant.id()) ? Mono.just(tenant) : Mono.empty();
        return new TenantMustBeActiveRuleImpl(repository);
    }
}
