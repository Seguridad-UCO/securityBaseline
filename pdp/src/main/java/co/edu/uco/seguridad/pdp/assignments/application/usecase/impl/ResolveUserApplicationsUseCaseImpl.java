package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ResolveUserApplicationsRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.UserApplicationRolesResponse;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.ProfileAssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ResolveUserApplicationsUseCase;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.ProfileOwnershipQuery;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.ProfileRolesLookupValidator;
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
    private final ProfileRolesLookupValidator profileRoles;
    private final TimeProvider time;

    public ResolveUserApplicationsUseCaseImpl(AssignmentRepository assignments, ProfileAssignmentRepository profileAssignments,
                                              ProfileRolesLookupValidator profileRoles, TimeProvider time) {
        this.assignments = Objects.requireNonNull(assignments); this.profileAssignments = Objects.requireNonNull(profileAssignments);
        this.profileRoles = Objects.requireNonNull(profileRoles); this.time = Objects.requireNonNull(time);
    }

    @Override public Mono<UserApplicationRolesResponse> execute(ResolveUserApplicationsRequest input) {
        var now = time.now();
        Mono<Map<ApplicationId, Set<RoleId>>> direct = assignments.findActiveByUserAndTenant(input.userId(), input.tenantId(), now)
                .collect(HashMap<ApplicationId, Set<RoleId>>::new,
                        (result, assignment) -> result.computeIfAbsent(assignment.applicationId(), ignored -> new HashSet<>()).add(assignment.roleId()))
                .map(Map::copyOf);
        Mono<Map<ApplicationId, Set<RoleId>>> fromProfiles = profileAssignments.findActiveByUserAndTenant(input.userId(), input.tenantId(), now)
                .flatMap(profile -> profileRoles.execute(new ProfileOwnershipQuery(input.tenantId(), profile.profileId()))
                        .map(roles -> Map.entry(profile.applicationId(), roles)))
                .collect(HashMap<ApplicationId, Set<RoleId>>::new,
                        (result, entry) -> result.computeIfAbsent(entry.getKey(), ignored -> new HashSet<>()).addAll(entry.getValue()))
                .map(Map::copyOf);
        return Mono.zip(direct, fromProfiles).map(pair -> {
            Map<ApplicationId, Set<RoleId>> all = new HashMap<>(pair.getT1());
            pair.getT2().forEach((application, roles) -> all.computeIfAbsent(application, ignored -> new HashSet<>()).addAll(roles));
            return new UserApplicationRolesResponse(all);
        });
    }
}
