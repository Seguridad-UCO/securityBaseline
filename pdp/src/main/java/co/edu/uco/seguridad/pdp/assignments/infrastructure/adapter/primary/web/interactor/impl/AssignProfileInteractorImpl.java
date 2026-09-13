package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.assignments.application.usecase.AssignProfileUseCase;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.AssignProfileRawRequest;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.response.ProfileAssignmentWebResponse;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.AssignProfileInteractor;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper.AssignProfileRequestMapper;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper.ProfileAssignmentResponseMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Lee el principal (el inquilino nunca viene del cuerpo), mapea raw a request, ejecuta y aplana la respuesta. */
public final class AssignProfileInteractorImpl implements AssignProfileInteractor {

    private final AssignProfileUseCase useCase;

    public AssignProfileInteractorImpl(AssignProfileUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.ASSIGN_PROFILE_USE_CASE);
    }

    @Override
    public Mono<ProfileAssignmentWebResponse> execute(AssignProfileRawRequest input) {
        return SecurityContext.currentPrincipal()
                .map(principal -> AssignProfileRequestMapper.toRequest(input, principal.tenantId()))
                .flatMap(useCase::execute)
                .map(ProfileAssignmentResponseMapper::toResponse);
    }
}
