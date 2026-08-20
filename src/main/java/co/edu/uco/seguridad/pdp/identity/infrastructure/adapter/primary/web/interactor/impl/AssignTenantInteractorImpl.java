package co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.identity.application.usecase.AssignTenantUseCase;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.dto.request.raw.AssignTenantRawRequest;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.dto.response.UserWebResponse;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.interactor.AssignTenantInteractor;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.mapper.AssignTenantRequestMapper;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.mapper.UserResponseMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class AssignTenantInteractorImpl implements AssignTenantInteractor {

    private final AssignTenantUseCase useCase;

    public AssignTenantInteractorImpl(AssignTenantUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.REGISTER_USE_CASE);
    }

    @Override
    public Mono<UserWebResponse> execute(AssignTenantRawRequest raw) {
        return useCase.execute(AssignTenantRequestMapper.toRequest(raw)).map(UserResponseMapper::toResponse);
    }
}
