package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.assignments.application.usecase.RevokeProfileAssignmentUseCase;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.RevokeProfileAssignmentRawRequest;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.RevokeProfileAssignmentInteractor;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper.RevokeProfileAssignmentRequestMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Lee el principal (el inquilino nunca viene del cuerpo), mapea raw a request y ejecuta. */
public final class RevokeProfileAssignmentInteractorImpl implements RevokeProfileAssignmentInteractor {

    private final RevokeProfileAssignmentUseCase useCase;

    public RevokeProfileAssignmentInteractorImpl(RevokeProfileAssignmentUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.REVOKE_PROFILE_ASSIGNMENT_USE_CASE);
    }

    @Override
    public Mono<Void> execute(RevokeProfileAssignmentRawRequest input) {
        return SecurityContext.currentPrincipal()
                .map(principal -> RevokeProfileAssignmentRequestMapper.toRequest(input, principal.tenantId()))
                .flatMap(useCase::execute);
    }
}
