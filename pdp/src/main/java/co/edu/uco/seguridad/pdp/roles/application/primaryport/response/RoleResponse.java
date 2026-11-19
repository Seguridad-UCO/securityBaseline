package co.edu.uco.seguridad.pdp.roles.application.primaryport.response;

import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Salida del núcleo: value objects, sin aplanar. El aplanado es del adaptador web.
 */
public record RoleResponse(RoleId id, RoleName name, RoleScope scope, Set<ResourceId> resources, Instant registeredAt) {

    public RoleResponse {
        Objects.requireNonNull(id, RequiredArgumentMessages.ROLE_ID);
        Objects.requireNonNull(name, RequiredArgumentMessages.ROLE_NAME);
        Objects.requireNonNull(scope, RequiredArgumentMessages.ROLE_SCOPE);
        resources = Set.copyOf(Objects.requireNonNull(resources, RequiredArgumentMessages.ROLE_RESOURCES));
        Objects.requireNonNull(registeredAt, RequiredArgumentMessages.REGISTERED_AT);
    }
}
