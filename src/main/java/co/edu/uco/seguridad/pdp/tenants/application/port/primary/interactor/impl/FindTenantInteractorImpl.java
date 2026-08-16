package co.edu.uco.seguridad.pdp.tenants.application.port.primary.interactor.impl;

import co.edu.uco.seguridad.crosscutting.messages.RequiredArgumentMessages;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.tenants.application.port.primary.dto.response.TenantResponse;
import co.edu.uco.seguridad.pdp.tenants.application.port.primary.interactor.FindTenantInteractor;
import co.edu.uco.seguridad.pdp.tenants.application.usecase.FindTenantUseCase;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class FindTenantInteractorImpl implements FindTenantInteractor {

    private final FindTenantUseCase useCase;

    public FindTenantInteractorImpl(FindTenantUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.SEARCH_USE_CASE);
    }

    @Override
    public Mono<TenantResponse> execute(TenantId tenantId) {
        return useCase.execute(tenantId);
    }
}
