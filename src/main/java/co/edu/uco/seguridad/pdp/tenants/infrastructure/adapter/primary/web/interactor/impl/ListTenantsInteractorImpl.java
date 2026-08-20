package co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.tenants.application.usecase.ListTenantsUseCase;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.dto.response.TenantWebResponse;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.interactor.ListTenantsInteractor;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.mapper.TenantResponseMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

public final class ListTenantsInteractorImpl implements ListTenantsInteractor {

    private final ListTenantsUseCase useCase;

    public ListTenantsInteractorImpl(ListTenantsUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.SEARCH_USE_CASE);
    }

    @Override
    public Mono<List<TenantWebResponse>> execute() {
        return useCase.execute().map(TenantResponseMapper::toResponseList);
    }
}
