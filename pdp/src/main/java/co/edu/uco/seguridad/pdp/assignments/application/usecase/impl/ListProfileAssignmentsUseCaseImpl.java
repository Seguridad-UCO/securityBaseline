package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ListProfileAssignmentsRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ProfileAssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.ProfileAssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ListProfileAssignmentsUseCase;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Consulta paginada del catálogo de asignaciones de un perfil. Delega y proyecta; el orden lo fija el adaptador.
 */
public final class ListProfileAssignmentsUseCaseImpl implements ListProfileAssignmentsUseCase {

    private final ProfileAssignmentRepository repository;

    public ListProfileAssignmentsUseCaseImpl(ProfileAssignmentRepository repository) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.PROFILE_ASSIGNMENT_REPOSITORY);
    }

    @Override
    public Mono<ResultPage<ProfileAssignmentResponse>> execute(ListProfileAssignmentsRequest input) {
        return repository.findBy(input.criteria(), input.window())
                .map(page -> page.map(profileAssignment -> new ProfileAssignmentResponse(profileAssignment.id(),
                        profileAssignment.userId(), profileAssignment.tenantId(), profileAssignment.applicationId(),
                        profileAssignment.profileId(), profileAssignment.generatedAssignmentIds(),
                        profileAssignment.validity().validFrom(), profileAssignment.validity().validUntil())));
    }
}
