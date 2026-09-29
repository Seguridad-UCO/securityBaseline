package co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.roles.application.usecase.ListRolesUseCase;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.request.raw.ListRolesRawRequest;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.response.RoleWebResponse;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.interactor.ListRolesInteractor;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.mapper.ListRolesRequestMapper;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.mapper.RoleResponseMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import co.edu.uco.seguridad.shared.web.PageResponse;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Lee el principal, resuelve la ventana con el mapper, ejecuta y proyecta ResultPage a PageResponse.
 */
public final class ListRolesInteractorImpl implements ListRolesInteractor {

    private final ListRolesUseCase useCase;

    public ListRolesInteractorImpl(ListRolesUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.LIST_ROLES_USE_CASE);
    }

    @Override
    public Mono<PageResponse<RoleWebResponse>> execute(ListRolesRawRequest input) {
        return SecurityContext.currentPrincipal()
                .map(principal -> ListRolesRequestMapper.toRequest(input, principal.tenantId()))
                .flatMap(useCase::execute)
                .map(page -> new PageResponse<>(
                        page.content().stream().map(RoleResponseMapper::toResponse).toList(),
                        page.total(),
                        page.window().page(),
                        page.window().offset(),
                        page.window().limit()));
    }
}
