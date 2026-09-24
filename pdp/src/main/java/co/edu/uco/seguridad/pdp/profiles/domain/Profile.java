package co.edu.uco.seguridad.pdp.profiles.domain;

import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.profiles.domain.model.ProfileName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;

import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Entrada del catálogo de perfiles (BC-05): un nombre, un alcance propio y el conjunto de roles que
 * agrupa. Espejo de {@code Role} (BC-04): ese conjunto es dato de catálogo, no una decisión — por
 * eso este agregado no tiene {@code authorizes(...)}. Ver PLAN-HU-011.md §4.
 */
public record Profile(ProfileId id, ProfileName name, RoleScope scope, Set<RoleId> roles, Instant registeredAt) {

    public Profile {
        Objects.requireNonNull(id, RequiredArgumentMessages.PROFILE_ID);
        Objects.requireNonNull(name, RequiredArgumentMessages.PROFILE_NAME);
        Objects.requireNonNull(scope, RequiredArgumentMessages.PROFILE_SCOPE);
        roles = Set.copyOf(Objects.requireNonNull(roles, RequiredArgumentMessages.PROFILE_ROLES));
        Objects.requireNonNull(registeredAt, RequiredArgumentMessages.REGISTERED_AT);
    }

    public static Profile define(ProfileId id, ProfileName name, RoleScope scope, Instant registeredAt) {
        return new Profile(id, name, scope, Set.of(), registeredAt);
    }

    /** Nuevo perfil con el rol añadido. Idempotente: agregar dos veces deja el conjunto igual. */
    public Profile withRole(RoleId roleId) {
        Set<RoleId> withNewRole = new HashSet<>(roles);
        withNewRole.add(roleId);
        return new Profile(id, name, scope, Set.copyOf(withNewRole), registeredAt);
    }

    /** Nuevo perfil sin el rol. Es idempotente para que DELETE sea seguro al repetirlo. */
    public Profile withoutRole(RoleId roleId) {
        Set<RoleId> remaining = new HashSet<>(roles);
        remaining.remove(roleId);
        return new Profile(id, name, scope, Set.copyOf(remaining), registeredAt);
    }

    public Profile withName(ProfileName name) {
        return new Profile(id, name, scope, roles, registeredAt);
    }
}
