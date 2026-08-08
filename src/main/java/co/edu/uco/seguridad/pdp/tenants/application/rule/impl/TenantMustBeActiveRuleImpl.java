package co.edu.uco.seguridad.pdp.tenants.application.rule.impl;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.tenants.application.exception.TenantNotFoundException;
import co.edu.uco.seguridad.pdp.tenants.application.port.primary.dto.response.TenantResponse;
import co.edu.uco.seguridad.pdp.tenants.application.port.secondary.repository.TenantRepository;
import co.edu.uco.seguridad.pdp.tenants.application.rule.TenantMustBeActiveRule;
import co.edu.uco.seguridad.pdp.tenants.application.rule.TenantStatusMustBeActiveRule;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Regla compuesta respaldada por repositorio: carga el inquilino y delega el chequeo de estado
 * a {@link TenantStatusMustBeActiveRule}.
 */
public final class TenantMustBeActiveRuleImpl implements TenantMustBeActiveRule {

    private final TenantRepository repository;
    private final TenantStatusMustBeActiveRule statusMustBeActive;

    public TenantMustBeActiveRuleImpl(TenantRepository repository,
                                      TenantStatusMustBeActiveRule statusMustBeActive) {
        this.repository = Objects.requireNonNull(repository, "se requiere repositorio de inquilino");
        this.statusMustBeActive = Objects.requireNonNull(statusMustBeActive, "se requiere regla de estado activo");
    }

    @Override
    public Mono<TenantResponse> execute(TenantId tenantId) {
        return repository.findById(tenantId)
                .switchIfEmpty(Mono.error(() -> new TenantNotFoundException(tenantId)))
                .map(tenant -> {
                    statusMustBeActive.execute(tenant);
                    return new TenantResponse(tenant.id(), tenant.status());
                });
    }
}
