package co.edu.uco.seguridad.pdp.assignments.application.primaryport.response;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Roles efectivos por aplicación, sin datos de presentación ni identificadores HTTP. */
public record UserApplicationRolesResponse(Map<ApplicationId, Set<RoleId>> rolesByApplication) {
    public UserApplicationRolesResponse {
        rolesByApplication = rolesByApplication.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Set.copyOf(entry.getValue())));
    }
}
