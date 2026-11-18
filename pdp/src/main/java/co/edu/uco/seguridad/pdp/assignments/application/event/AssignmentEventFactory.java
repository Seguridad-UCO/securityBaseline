package co.edu.uco.seguridad.pdp.assignments.application.event;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationDetailsLookupValidator;
import co.edu.uco.seguridad.pdp.assignments.domain.Assignment;
import co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignment;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.ProfileNamesLookupValidator;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleNamesLookupValidator;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Congela los nombres antes de persistir la outbox; el dispatcher no consulta catálogos. */
public final class AssignmentEventFactory {
    private final ApplicationDetailsLookupValidator applications; private final RoleNamesLookupValidator roles;
    private final ProfileNamesLookupValidator profiles; private final IdentifierGenerator identifiers;
    public AssignmentEventFactory(ApplicationDetailsLookupValidator applications, RoleNamesLookupValidator roles,
                                  ProfileNamesLookupValidator profiles, IdentifierGenerator identifiers) {
        this.applications = applications; this.roles = roles; this.profiles = profiles; this.identifiers = identifiers;
    }
    public Mono<AssignmentChangedEventV1> role(String type, Assignment assignment, Instant occurredOn) {
        return Mono.zip(applications.execute(assignment.applicationId()), roles.execute(Set.of(assignment.roleId())))
                .map(values -> new AssignmentChangedEventV1(identifiers.next().toString(), 1, type, occurredOn,
                        assignment.tenantId().value(), assignment.applicationId().value().toString(), values.getT1().name().value(),
                        assignment.userId().value().toString(), null, assignment.id().value().toString(), assignment.roleId().value().toString(),
                        values.getT2().stream().findFirst().orElse("unknown"), null, null, null, List.of()));
    }
    public Mono<AssignmentChangedEventV1> profile(String type, ProfileAssignment profile, List<Assignment> materialized, Instant occurredOn) {
        return Mono.zip(applications.execute(profile.applicationId()), profiles.execute(Set.of(profile.profileId())))
                .flatMap(headers -> Flux.fromIterable(materialized)
                        .concatMap(assignment -> roles.execute(Set.of(assignment.roleId())).map(names ->
                                new AssignmentChangedEventV1.RoleAssignment(assignment.id().value().toString(), assignment.roleId().value().toString(), names.stream().findFirst().orElse("unknown"))))
                        .collectList()
                        .map(roleAssignments -> new AssignmentChangedEventV1(identifiers.next().toString(), 1, type, occurredOn,
                                profile.tenantId().value(), profile.applicationId().value().toString(), headers.getT1().name().value(),
                                profile.userId().value().toString(), null, null, null, null, profile.id().value().toString(),
                                profile.profileId().value().toString(), headers.getT2().stream().findFirst().orElse("unknown"), roleAssignments)));
    }
}
