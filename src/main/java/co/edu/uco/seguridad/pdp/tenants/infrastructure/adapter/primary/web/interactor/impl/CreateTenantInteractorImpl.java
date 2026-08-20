package co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.tenants.application.usecase.CreateTenantUseCase;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.dto.request.raw.CreateTenantRawRequest;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.dto.response.TenantWebResponse;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.interactor.CreateTenantInteractor;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.mapper.CreateTenantRequestMapper;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.mapper.TenantResponseMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Mapea el payload HTTP → DTO tipado, ejecuta el caso de uso y proyecta la respuesta de aplicación →
 * respuesta HTTP. Operación administrativa: no depende del tenant del llamador.
 */
public final class CreateTenantInteractorImpl implements CreateTenantInteractor {

    private final CreateTenantUseCase useCase;

    public CreateTenantInteractorImpl(CreateTenantUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.REGISTER_USE_CASE);
    }

    @Override
    public Mono<TenantWebResponse> execute(CreateTenantRawRequest raw) {
        return useCase.execute(CreateTenantRequestMapper.toRequest(raw)).map(TenantResponseMapper::toResponse);
    }
}
