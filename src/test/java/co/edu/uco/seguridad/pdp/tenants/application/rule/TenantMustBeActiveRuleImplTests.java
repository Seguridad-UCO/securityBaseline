package co.edu.uco.seguridad.pdp.tenants.application.rule;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.tenants.application.exception.TenantNotActiveException;
import co.edu.uco.seguridad.pdp.tenants.application.exception.TenantNotFoundException;
import co.edu.uco.seguridad.pdp.tenants.application.port.secondary.repository.TenantRepository;
import co.edu.uco.seguridad.pdp.tenants.application.rule.impl.TenantMustBeActiveRuleImpl;
import co.edu.uco.seguridad.pdp.tenants.application.rule.impl.TenantStatusMustBeActiveRuleImpl;
import co.edu.uco.seguridad.pdp.tenants.domain.Tenant;
import co.edu.uco.seguridad.pdp.tenants.domain.TenantName;
import co.edu.uco.seguridad.pdp.tenants.domain.TenantStatus;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
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
    private static final TenantName NAME = new TenantName("Universidad UCO");

    @Test
    void passes_and_returns_the_snapshot_for_an_active_tenant() {
        TenantMustBeActiveRuleImpl rule = ruleFor(new Tenant(TENANT, NAME, TenantStatus.ACTIVE));

        StepVerifier.create(rule.execute(TENANT))
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(TENANT);
                    assertThat(dto.active()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void reports_an_unknown_tenant_as_not_found() {
        TenantMustBeActiveRuleImpl rule = new TenantMustBeActiveRuleImpl(
                fakeRepository(null), new TenantStatusMustBeActiveRuleImpl());

        StepVerifier.create(rule.execute(TENANT))
                .expectError(TenantNotFoundException.class)
                .verify();
    }

    @Test
    void reports_a_suspended_tenant_as_not_active() {
        TenantMustBeActiveRuleImpl rule = ruleFor(new Tenant(TENANT, NAME, TenantStatus.SUSPENDED));

        StepVerifier.create(rule.execute(TENANT))
                .expectError(TenantNotActiveException.class)
                .verify();
    }

    private static TenantMustBeActiveRuleImpl ruleFor(Tenant tenant) {
        return new TenantMustBeActiveRuleImpl(fakeRepository(tenant), new TenantStatusMustBeActiveRuleImpl());
    }

    private static TenantRepository fakeRepository(Tenant tenant) {
        return new TenantRepository() {
            @Override
            public Mono<Tenant> findById(TenantId tenantId) {
                return tenant != null && tenant.id().equals(tenantId) ? Mono.just(tenant) : Mono.empty();
            }

            @Override
            public Mono<Boolean> existsById(TenantId tenantId) {
                return Mono.just(tenant != null && tenant.id().equals(tenantId));
            }

            @Override
            public Mono<Tenant> save(Tenant toSave) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Flux<Tenant> findAll() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
