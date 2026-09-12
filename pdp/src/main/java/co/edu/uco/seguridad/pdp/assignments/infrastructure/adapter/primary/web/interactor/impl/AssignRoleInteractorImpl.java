package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.assignments.application.usecase.AssignRoleUseCase;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.AssignRoleRawRequest;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.response.AssignmentWebResponse;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.AssignRoleInteractor;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper.AssignRoleRequestMapper;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper.AssignmentResponseMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Lee el principal (el inquilino nunca viene del cuerpo), mapea raw a request, ejecuta y aplana la respuesta. */
public final class AssignRoleInteractorImpl implements AssignRoleInteractor {

    private final AssignRoleUseCase useCase;

    public AssignRoleInteractorImpl(AssignRoleUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.ASSIGN_ROLE_USE_CASE);
    }

    @Override
    public Mono<AssignmentWebResponse> execute(AssignRoleRawRequest input) {
        return SecurityContext.currentPrincipal()
                .map(principal -> AssignRoleRequestMapper.toRequest(input, principal.tenantId()))
                .flatMap(useCase::execute)
                .map(AssignmentResponseMapper::toResponse);
    }
}
