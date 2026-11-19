package co.edu.uco.seguridad.pdp.roles.domain;

import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Entrada del catálogo de roles (BC-04): un nombre, un alcance y el conjunto de recursos
 * protegidos que declara autorizar. Ese conjunto es dato de catálogo que viaja a OPA como
 * evidencia: este agregado no decide nada con él, y por eso no tiene {@code authorizes(...)}.
 */
public record Role(RoleId id, RoleName name, RoleScope scope, Set<ResourceId> resources, Instant registeredAt) {

    public Role {
        Objects.requireNonNull(id, RequiredArgumentMessages.ROLE_ID);
        Objects.requireNonNull(name, RequiredArgumentMessages.ROLE_NAME);
        Objects.requireNonNull(scope, RequiredArgumentMessages.ROLE_SCOPE);
        resources = Set.copyOf(Objects.requireNonNull(resources, RequiredArgumentMessages.ROLE_RESOURCES));
        Objects.requireNonNull(registeredAt, RequiredArgumentMessages.REGISTERED_AT);
    }

    public static Role define(RoleId id, RoleName name, RoleScope scope, Instant registeredAt) {
        return new Role(id, name, scope, Set.of(), registeredAt);
    }

    /**
     * Nuevo rol con el recurso añadido. Idempotente: conceder dos veces deja el conjunto igual.
     */
    public Role withResource(ResourceId resourceId) {
        Set<ResourceId> withNewResource = new HashSet<>(resources);
        withNewResource.add(resourceId);
        return new Role(id, name, scope, Set.copyOf(withNewResource), registeredAt);
    }

    /**
     * Nuevo rol sin el recurso. Es idempotente para que DELETE sea seguro al repetirlo.
     */
    public Role withoutResource(ResourceId resourceId) {
        Set<ResourceId> remaining = new HashSet<>(resources);
        remaining.remove(resourceId);
        return new Role(id, name, scope, Set.copyOf(remaining), registeredAt);
    }

    public Role withName(RoleName name) {
        return new Role(id, name, scope, resources, registeredAt);
    }
}
