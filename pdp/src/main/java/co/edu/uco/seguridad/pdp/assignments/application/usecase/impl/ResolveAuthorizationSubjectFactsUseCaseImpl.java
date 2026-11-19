package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ResolveAuthorizationSubjectFactsRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AuthorizationSubjectFactsResponse;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.ProfileAssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ResolveAuthorizationSubjectFactsUseCase;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.ProfileOwnershipQuery;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.ProfileNamesLookupValidator;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.ProfileRolesLookupValidator;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleNamesLookupValidator;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleResourcesLookupValidator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;

public final class ResolveAuthorizationSubjectFactsUseCaseImpl implements ResolveAuthorizationSubjectFactsUseCase {
    private final AssignmentRepository assignments;
    private final ProfileAssignmentRepository profiles;
    private final ProfileRolesLookupValidator profileRoles;
    private final ProfileNamesLookupValidator profileNames;
    private final RoleNamesLookupValidator roleNames;
    private final RoleResourcesLookupValidator resources;
    private final TimeProvider time;

    public ResolveAuthorizationSubjectFactsUseCaseImpl(AssignmentRepository assignments, ProfileAssignmentRepository profiles,
                                                       ProfileRolesLookupValidator profileRoles, ProfileNamesLookupValidator profileNames, RoleNamesLookupValidator roleNames,
                                                       RoleResourcesLookupValidator resources, TimeProvider time) {
        this.assignments = assignments;
        this.profiles = profiles;
        this.profileRoles = profileRoles;
        this.profileNames = profileNames;
        this.roleNames = roleNames;
        this.resources = resources;
        this.time = time;
    }

    @Override
    public Mono<AuthorizationSubjectFactsResponse> execute(ResolveAuthorizationSubjectFactsRequest input) {
        var now = time.now();
        return Mono.zip(assignments.findActiveRoleIdsFor(input.userId(), input.applicationId(), now),
                        profiles.findActiveProfileIdsFor(input.userId(), input.applicationId(), now))
                .flatMap(pair -> Flux.fromIterable(pair.getT2()).flatMap(id -> profileRoles.execute(new ProfileOwnershipQuery(input.tenantId(), id)))
                        .reduce(new HashSet<RoleId>(pair.getT1()), (all, fromProfile) -> {
                            all.addAll(fromProfile);
                            return all;
                        })
                        .flatMap(effective -> Mono.zip(roleNames.execute(effective), profileNames.execute(pair.getT2()), resources.execute(effective)))
                        .map(values -> new AuthorizationSubjectFactsResponse(values.getT1(), values.getT2(), values.getT3())));
    }
}
