package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ResolveUserApplicationsRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.UserApplicationAssignmentsResponse;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.ProfileAssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ResolveUserApplicationsUseCase;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ResolveUserApplicationsUseCaseImpl implements ResolveUserApplicationsUseCase {
    private final AssignmentRepository assignments;
    private final ProfileAssignmentRepository profileAssignments;
    private final TimeProvider time;

    public ResolveUserApplicationsUseCaseImpl(AssignmentRepository assignments, ProfileAssignmentRepository profileAssignments,
                                              TimeProvider time) {
        this.assignments = Objects.requireNonNull(assignments); this.profileAssignments = Objects.requireNonNull(profileAssignments);
        this.time = Objects.requireNonNull(time);
    }

    @Override public Mono<UserApplicationAssignmentsResponse> execute(ResolveUserApplicationsRequest input) {
        var now = time.now();
        Mono<Map<ApplicationId, Set<RoleId>>> direct = assignments.findActiveByUserAndTenant(input.userId(), input.tenantId(), now)
                .collect(HashMap<ApplicationId, Set<RoleId>>::new,
                        (result, assignment) -> result.computeIfAbsent(assignment.applicationId(), ignored -> new HashSet<>()).add(assignment.roleId()))
                .map(Map::copyOf);
        Mono<Map<ApplicationId, Set<co.edu.uco.seguridad.pdp.commons.model.ProfileId>>> profiles = profileAssignments
                .findActiveByUserAndTenant(input.userId(), input.tenantId(), now)
                .collect(HashMap<ApplicationId, Set<co.edu.uco.seguridad.pdp.commons.model.ProfileId>>::new,
                        (result, assignment) -> result.computeIfAbsent(assignment.applicationId(), ignored -> new HashSet<>())
                                .add(assignment.profileId()))
                .map(Map::copyOf);
        return Mono.zip(direct, profiles).map(pair -> {
            Map<ApplicationId, UserApplicationAssignmentsResponse.ApplicationAssignments> all = new HashMap<>();
            Set<ApplicationId> applications = new HashSet<>(pair.getT1().keySet());
            applications.addAll(pair.getT2().keySet());
            applications.forEach(application -> all.put(application,
                    new UserApplicationAssignmentsResponse.ApplicationAssignments(
                            pair.getT1().getOrDefault(application, Set.of()),
                            pair.getT2().getOrDefault(application, Set.of()))));
            return new UserApplicationAssignmentsResponse(all);
        });
    }
}
