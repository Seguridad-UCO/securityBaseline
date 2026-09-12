package co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.roles.application.usecase.GrantResourceToRoleUseCase;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.request.raw.GrantResourceRawRequest;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.response.RoleWebResponse;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.interactor.GrantResourceToRoleInteractor;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.mapper.GrantResourceRequestMapper;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.mapper.RoleResponseMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Lee el principal (el inquilino nunca viene del cuerpo), mapea raw a request, ejecuta y aplana la respuesta. */
public final class GrantResourceToRoleInteractorImpl implements GrantResourceToRoleInteractor {

    private final GrantResourceToRoleUseCase useCase;

    public GrantResourceToRoleInteractorImpl(GrantResourceToRoleUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.GRANT_RESOURCE_TO_ROLE_USE_CASE);
    }

    @Override
    public Mono<RoleWebResponse> execute(GrantResourceRawRequest input) {
        return SecurityContext.currentPrincipal()
                .map(principal -> GrantResourceRequestMapper.toRequest(input, principal.tenantId()))
                .flatMap(useCase::execute)
                .map(RoleResponseMapper::toResponse);
    }
}
