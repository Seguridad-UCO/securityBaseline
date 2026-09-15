package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.assignments.application.usecase.ListProfileAssignmentsUseCase;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.ListProfileAssignmentsRawRequest;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.response.ProfileAssignmentWebResponse;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.ListProfileAssignmentsInteractor;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper.ListProfileAssignmentsRequestMapper;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper.ProfileAssignmentResponseMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import co.edu.uco.seguridad.shared.web.PageResponse;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Lee el principal, resuelve la ventana con el mapper, ejecuta y proyecta ResultPage a PageResponse. */
public final class ListProfileAssignmentsInteractorImpl implements ListProfileAssignmentsInteractor {

    private final ListProfileAssignmentsUseCase useCase;

    public ListProfileAssignmentsInteractorImpl(ListProfileAssignmentsUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.LIST_PROFILE_ASSIGNMENTS_USE_CASE);
    }

    @Override
    public Mono<PageResponse<ProfileAssignmentWebResponse>> execute(ListProfileAssignmentsRawRequest input) {
        return SecurityContext.currentPrincipal()
                .map(principal -> ListProfileAssignmentsRequestMapper.toRequest(input, principal.tenantId()))
                .flatMap(useCase::execute)
                .map(page -> new PageResponse<>(
                        page.content().stream().map(ProfileAssignmentResponseMapper::toResponse).toList(),
                        page.total(),
                        page.window().page(),
                        page.window().offset(),
                        page.window().limit()));
    }
}
