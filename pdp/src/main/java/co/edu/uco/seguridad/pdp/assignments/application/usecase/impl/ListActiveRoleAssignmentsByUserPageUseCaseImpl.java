package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ListActiveAssignmentsPageRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ListActiveRoleAssignmentsByUserPageUseCase;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class ListActiveRoleAssignmentsByUserPageUseCaseImpl implements ListActiveRoleAssignmentsByUserPageUseCase {

    private final AssignmentRepository repository;
    private final TimeProvider time;

    public ListActiveRoleAssignmentsByUserPageUseCaseImpl(AssignmentRepository repository, TimeProvider time) {
        this.repository = Objects.requireNonNull(repository);
        this.time = Objects.requireNonNull(time);
    }

    @Override
    public Mono<ResultPage<AssignmentResponse>> execute(ListActiveAssignmentsPageRequest<UserId> input) {
        return repository.findActivePageByUserAndApplication(input.key(), input.tenantId(), input.applicationId(), time.now(), input.window())
                .map(page -> page.map(a -> new AssignmentResponse(a.id(), a.userId(), a.tenantId(), a.applicationId(), a.roleId(),
                a.validity().validFrom(), a.validity().validUntil())));
    }
}
