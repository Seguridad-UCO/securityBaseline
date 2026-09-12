package co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.roles.application.usecase.DefineRoleUseCase;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.request.raw.DefineRoleRawRequest;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.response.RoleWebResponse;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.interactor.DefineRoleInteractor;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.mapper.DefineRoleRequestMapper;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.mapper.RoleResponseMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Lee el principal (el inquilino nunca viene del cuerpo), mapea raw a request, ejecuta y aplana la respuesta. */
public final class DefineRoleInteractorImpl implements DefineRoleInteractor {

    private final DefineRoleUseCase useCase;

    public DefineRoleInteractorImpl(DefineRoleUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.DEFINE_ROLE_USE_CASE);
    }

    @Override
    public Mono<RoleWebResponse> execute(DefineRoleRawRequest input) {
        return SecurityContext.currentPrincipal()
                .map(principal -> DefineRoleRequestMapper.toRequest(input, principal.tenantId()))
                .flatMap(useCase::execute)
                .map(RoleResponseMapper::toResponse);
    }
}
