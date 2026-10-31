package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ListActiveAssignmentsPageRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ProfileAssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.ProfileAssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ListActiveProfileAssignmentsByUserPageUseCase;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class ListActiveProfileAssignmentsByUserPageUseCaseImpl implements ListActiveProfileAssignmentsByUserPageUseCase {

    private final ProfileAssignmentRepository repository;
    private final TimeProvider time;

    public ListActiveProfileAssignmentsByUserPageUseCaseImpl(ProfileAssignmentRepository repository, TimeProvider time) {
        this.repository = Objects.requireNonNull(repository);
        this.time = Objects.requireNonNull(time);
    }

    @Override
    public Mono<ResultPage<ProfileAssignmentResponse>> execute(ListActiveAssignmentsPageRequest<UserId> input) {
        return repository.findActivePageByUserAndApplication(input.key(), input.tenantId(), input.applicationId(), time.now(), input.window())
                .map(page -> page.map(a -> new ProfileAssignmentResponse(a.id(), a.userId(), a.tenantId(), a.applicationId(),
                a.profileId(), a.generatedAssignmentIds(), a.validity().validFrom(), a.validity().validUntil())));
    }
}
