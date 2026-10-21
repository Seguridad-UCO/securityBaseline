package co.edu.uco.seguridad.pdp.tenants.application.usecase.impl;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.tenants.application.primaryport.response.TenantNameResponse;
import co.edu.uco.seguridad.pdp.tenants.application.secondaryport.repository.TenantRepository;
import co.edu.uco.seguridad.pdp.tenants.application.usecase.FindTenantNameByCodeUseCase;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class FindTenantNameByCodeUseCaseImpl implements FindTenantNameByCodeUseCase {
    private final TenantRepository tenants;

    public FindTenantNameByCodeUseCaseImpl(TenantRepository tenants) { this.tenants = Objects.requireNonNull(tenants); }

    @Override public Mono<TenantNameResponse> execute(TenantId tenantId) {
        return tenants.findById(tenantId).map(tenant -> new TenantNameResponse(tenant.name().value()));
    }
}
