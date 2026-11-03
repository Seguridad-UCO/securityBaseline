package co.edu.uco.seguridad.pdp.profiles.application.primaryport.response;

import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.profiles.domain.model.ProfileName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;

import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

public record ProfileResponse(ProfileId id, ProfileName name, RoleScope scope, Set<RoleId> roles,
                              Instant registeredAt) {

    public ProfileResponse {
        Objects.requireNonNull(id, RequiredArgumentMessages.PROFILE_ID);
        Objects.requireNonNull(name, RequiredArgumentMessages.PROFILE_NAME);
        Objects.requireNonNull(scope, RequiredArgumentMessages.PROFILE_SCOPE);
        roles = Set.copyOf(Objects.requireNonNull(roles, RequiredArgumentMessages.PROFILE_ROLES));
        Objects.requireNonNull(registeredAt, RequiredArgumentMessages.REGISTERED_AT);
    }
}
