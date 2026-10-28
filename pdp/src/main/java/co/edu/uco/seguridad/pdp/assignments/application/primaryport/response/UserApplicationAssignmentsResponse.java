package co.edu.uco.seguridad.pdp.assignments.application.primaryport.response;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Asignaciones vigentes del sujeto, conservando su aplicación y origen. */
public record UserApplicationAssignmentsResponse(Map<ApplicationId, ApplicationAssignments> assignmentsByApplication) {
    public UserApplicationAssignmentsResponse {
        assignmentsByApplication = assignmentsByApplication.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public record ApplicationAssignments(Set<RoleId> directRoleIds, Set<ProfileId> profileIds) {
        public ApplicationAssignments {
            directRoleIds = Set.copyOf(directRoleIds);
            profileIds = Set.copyOf(profileIds);
        }
    }
}
