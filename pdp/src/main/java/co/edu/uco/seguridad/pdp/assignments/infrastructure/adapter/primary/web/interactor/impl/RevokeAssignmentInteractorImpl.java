package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.assignments.application.usecase.RevokeAssignmentUseCase;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.RevokeAssignmentRawRequest;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.RevokeAssignmentInteractor;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper.RevokeAssignmentRequestMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Lee el principal (el inquilino nunca viene del cuerpo), mapea raw a request y ejecuta. */
public final class RevokeAssignmentInteractorImpl implements RevokeAssignmentInteractor {

    private final RevokeAssignmentUseCase useCase;

    public RevokeAssignmentInteractorImpl(RevokeAssignmentUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.REVOKE_ASSIGNMENT_USE_CASE);
    }

    @Override
    public Mono<Void> execute(RevokeAssignmentRawRequest input) {
        return SecurityContext.currentPrincipal()
                .map(principal -> RevokeAssignmentRequestMapper.toRequest(input, principal.tenantId()))
                .flatMap(useCase::execute);
    }
}
