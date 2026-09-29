package co.edu.uco.seguridad.pdp.profiles.application.primaryport.request;

import co.edu.uco.seguridad.pdp.profiles.domain.model.ProfileName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entrada tipada de DefineProfileUseCase. El alcance ya trae el inquilino del principal.
 */
public record DefineProfileRequest(ProfileName name, RoleScope scope) {

    public DefineProfileRequest {
        Objects.requireNonNull(name, RequiredArgumentMessages.PROFILE_NAME);
        Objects.requireNonNull(scope, RequiredArgumentMessages.PROFILE_SCOPE);
    }
}
