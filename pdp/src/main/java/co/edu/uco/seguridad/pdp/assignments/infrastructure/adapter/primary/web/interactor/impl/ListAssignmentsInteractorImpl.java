package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.assignments.application.usecase.ListAssignmentsUseCase;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.ListAssignmentsRawRequest;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.response.AssignmentWebResponse;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.ListAssignmentsInteractor;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper.AssignmentResponseMapper;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper.ListAssignmentsRequestMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import co.edu.uco.seguridad.shared.web.PageResponse;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Lee el principal, resuelve la ventana con el mapper, ejecuta y proyecta ResultPage a PageResponse. */
public final class ListAssignmentsInteractorImpl implements ListAssignmentsInteractor {

    private final ListAssignmentsUseCase useCase;

    public ListAssignmentsInteractorImpl(ListAssignmentsUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.LIST_ASSIGNMENTS_USE_CASE);
    }

    @Override
    public Mono<PageResponse<AssignmentWebResponse>> execute(ListAssignmentsRawRequest input) {
        return SecurityContext.currentPrincipal()
                .map(principal -> ListAssignmentsRequestMapper.toRequest(input, principal.tenantId()))
                .flatMap(useCase::execute)
                .map(page -> new PageResponse<>(
                        page.content().stream().map(AssignmentResponseMapper::toResponse).toList(),
                        page.total(),
                        page.window().page(),
                        page.window().offset(),
                        page.window().limit()));
    }
}
