package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ListActiveAssignmentsPageRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ListActiveRoleAssignmentsByRolePageUseCase;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class ListActiveRoleAssignmentsByRolePageUseCaseImpl implements ListActiveRoleAssignmentsByRolePageUseCase {

    private final AssignmentRepository repository;
    private final TimeProvider time;

    public ListActiveRoleAssignmentsByRolePageUseCaseImpl(AssignmentRepository repository, TimeProvider time) {
        this.repository = Objects.requireNonNull(repository);
        this.time = Objects.requireNonNull(time);
    }

    @Override
    public Mono<ResultPage<AssignmentResponse>> execute(ListActiveAssignmentsPageRequest<RoleId> input) {
        return repository.findActivePageByRoleAndApplication(input.key(), input.tenantId(), input.applicationId(), time.now(), input.window())
                .map(page -> page.map(a -> new AssignmentResponse(a.id(), a.userId(), a.tenantId(), a.applicationId(), a.roleId(),
                a.validity().validFrom(), a.validity().validUntil())));
    }
}
