package co.edu.uco.seguridad.pdp.tenants.application.rule.impl;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.tenants.application.rule.TenantMustBeActiveRule;
import co.edu.uco.seguridad.pdp.tenants.application.exception.TenantNotActiveException;
import co.edu.uco.seguridad.pdp.tenants.application.exception.TenantNotFoundException;
import co.edu.uco.seguridad.pdp.tenants.application.port.primary.dto.response.TenantResponse;
import co.edu.uco.seguridad.pdp.tenants.application.port.secondary.repository.TenantRepository;
import co.edu.uco.seguridad.pdp.tenants.domain.Tenant;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Regla respaldada por repositorio. Separa las dos formas en que la verificación puede fallar para que los
 * llamadores y clientes obtengan una causa, no solo un rechazo.
 */
public final class TenantMustBeActiveRuleImpl implements TenantMustBeActiveRule {

    private final TenantRepository repository;

    public TenantMustBeActiveRuleImpl(TenantRepository repository) {
        this.repository = Objects.requireNonNull(repository, "se requiere repositorio de inquilino");
    }

    @Override
    public Mono<TenantResponse> execute(TenantId tenantId) {
        return repository.findById(tenantId)
                .switchIfEmpty(Mono.error(() -> new TenantNotFoundException(tenantId)))
                .flatMap(this::requireActive);
    }

    private Mono<TenantResponse> requireActive(Tenant tenant) {
        return tenant.isActive()
                ? Mono.just(new TenantResponse(tenant.id(), tenant.status()))
                : Mono.error(new TenantNotActiveException(tenant.id(), tenant.status()));
    }
}
