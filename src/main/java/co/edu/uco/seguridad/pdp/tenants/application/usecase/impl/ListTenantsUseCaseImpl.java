package co.edu.uco.seguridad.pdp.tenants.application.usecase.impl;

import co.edu.uco.seguridad.pdp.tenants.application.port.primary.dto.response.TenantResponse;
import co.edu.uco.seguridad.pdp.tenants.application.port.secondary.repository.TenantRepository;
import co.edu.uco.seguridad.pdp.tenants.application.usecase.ListTenantsUseCase;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

public final class ListTenantsUseCaseImpl implements ListTenantsUseCase {

    private final TenantRepository repository;

    public ListTenantsUseCaseImpl(TenantRepository repository) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.TENANT_REPOSITORY);
    }

    @Override
    public Mono<List<TenantResponse>> execute() {
        return repository.findAll()
                .map(tenant -> new TenantResponse(tenant.id(), tenant.name(), tenant.status()))
                .collectList();
    }
}
