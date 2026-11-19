package co.edu.uco.seguridad.pdp.profiles.domain.rule.model;

import co.edu.uco.seguridad.pdp.profiles.domain.model.ProfileName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Hecho ya resuelto para ProfileNameMustBeUniqueInScopeRule: si el nombre ya está tomado en ese alcance.
 */
public record ProfileNameAvailability(ProfileName name, RoleScope scope, boolean taken) {

    public ProfileNameAvailability {
        Objects.requireNonNull(name, RequiredArgumentMessages.PROFILE_NAME);
        Objects.requireNonNull(scope, RequiredArgumentMessages.PROFILE_SCOPE);
    }
}
