package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ListAssignmentsRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ListAssignmentsUseCase;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Consulta paginada del catálogo de asignaciones de un rol. Delega y proyecta; el orden lo fija el adaptador.
 */
public final class ListAssignmentsUseCaseImpl implements ListAssignmentsUseCase {

    private final AssignmentRepository repository;

    public ListAssignmentsUseCaseImpl(AssignmentRepository repository) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.ASSIGNMENT_REPOSITORY);
    }

    @Override
    public Mono<ResultPage<AssignmentResponse>> execute(ListAssignmentsRequest input) {
        return repository.findBy(input.criteria(), input.window())
                .map(page -> page.map(assignment -> new AssignmentResponse(assignment.id(), assignment.userId(),
                        assignment.tenantId(), assignment.applicationId(), assignment.roleId(),
                        assignment.validity().validFrom(), assignment.validity().validUntil())));
    }
}
