package co.edu.uco.seguridad.pdp.assignments.application.primaryport.response;

import co.edu.uco.seguridad.pdp.commons.model.ResourceId;

import java.util.Set;

/**
 * Hechos actuales del PDP; no contiene ninguna decisión de política.
 */
public record AuthorizationSubjectFactsResponse(Set<String> roleNames, Set<String> profileNames,
                                                Set<ResourceId> effectiveResourceIds) {
    public AuthorizationSubjectFactsResponse {
        roleNames = Set.copyOf(roleNames);
        profileNames = Set.copyOf(profileNames);
        effectiveResourceIds = Set.copyOf(effectiveResourceIds);
    }
}
