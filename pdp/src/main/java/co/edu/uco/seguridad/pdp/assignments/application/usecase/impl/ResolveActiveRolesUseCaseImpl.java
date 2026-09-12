package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ResolveActiveRolesRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ActiveRolesResponse;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ResolveActiveRolesUseCase;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class ResolveActiveRolesUseCaseImpl implements ResolveActiveRolesUseCase {

    private final AssignmentRepository repository;
    private final TimeProvider time;

    public ResolveActiveRolesUseCaseImpl(AssignmentRepository repository, TimeProvider time) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.ASSIGNMENT_REPOSITORY);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<ActiveRolesResponse> execute(ResolveActiveRolesRequest input) {
        return repository.findActiveRoleIdsFor(input.userId(), input.applicationId(), time.now())
                .map(roleIds -> new ActiveRolesResponse(input.userId(), input.applicationId(), roleIds));
    }
}
