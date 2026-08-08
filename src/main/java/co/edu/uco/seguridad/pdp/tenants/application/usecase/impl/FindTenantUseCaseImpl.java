package co.edu.uco.seguridad.pdp.tenants.application.usecase.impl;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.tenants.application.port.primary.dto.response.TenantResponse;
import co.edu.uco.seguridad.pdp.tenants.application.port.secondary.repository.TenantRepository;
import co.edu.uco.seguridad.pdp.tenants.application.usecase.FindTenantUseCase;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Implementación de {@link FindTenantUseCase}: proyecta la entidad sobre su DTO de respuesta.
 */
public final class FindTenantUseCaseImpl implements FindTenantUseCase {

    private final TenantRepository repository;

    public FindTenantUseCaseImpl(TenantRepository repository) {
        this.repository = Objects.requireNonNull(repository, "se requiere repositorio de inquilino");
    }

    @Override
    public Mono<TenantResponse> execute(TenantId tenantId) {
        return repository.findById(tenantId).map(tenant -> new TenantResponse(tenant.id(), tenant.status()));
    }
}
