package co.edu.uco.seguridad.pdp.tenants.application.rule.validator;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.tenants.application.rule.validator.impl.TenantMustBeActiveValidatorImpl;
import co.edu.uco.seguridad.pdp.tenants.application.secondaryport.repository.TenantRepository;
import co.edu.uco.seguridad.pdp.tenants.domain.Tenant;
import co.edu.uco.seguridad.pdp.tenants.domain.exception.TenantNotActiveException;
import co.edu.uco.seguridad.pdp.tenants.domain.exception.TenantNotFoundException;
import co.edu.uco.seguridad.pdp.tenants.domain.model.TenantStatus;
import co.edu.uco.seguridad.pdp.tenants.domain.rule.impl.TenantMustExistRuleImpl;
import co.edu.uco.seguridad.pdp.tenants.domain.rule.impl.TenantStatusMustBeActiveRuleImpl;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * El validador con las reglas reales: no hay nada que sustituir en ellas, solo el repositorio.
 * Los dos modos de fallo se afirman por separado porque colapsarlos era exactamente lo que la
 * separación entre "no existe" y "no está activo" pretendía evitar.
 */
class TenantMustBeActiveValidatorTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");

    @Test
    void completes_for_an_active_tenant() {
        StepVerifier.create(validatorFor(TenantStatus.ACTIVE).execute(TENANT)).verifyComplete();
    }

    @Test
    void reports_an_unknown_tenant_as_not_found() {
        StepVerifier.create(validatorFor(null).execute(TENANT))
                .expectError(TenantNotFoundException.class)
                .verify();
    }

    @Test
    void reports_a_suspended_tenant_as_not_active() {
        StepVerifier.create(validatorFor(TenantStatus.SUSPENDED).execute(TENANT))
                .expectError(TenantNotActiveException.class)
                .verify();
    }

    private static TenantMustBeActiveValidator validatorFor(TenantStatus status) {
        return new TenantMustBeActiveValidatorImpl(fakeRepository(status), new TenantMustExistRuleImpl(),
                new TenantStatusMustBeActiveRuleImpl());
    }

    private static TenantRepository fakeRepository(TenantStatus status) {
        return new TenantRepository() {
            @Override
            public Mono<TenantStatus> findStatusById(TenantId tenantId) {
                return status == null ? Mono.empty() : Mono.just(status);
            }

            @Override
            public Mono<Boolean> existsById(TenantId tenantId) {
                return Mono.just(status != null);
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
