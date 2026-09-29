package co.edu.uco.seguridad.pdp.profiles.domain.exception;

import co.edu.uco.seguridad.pdp.commons.exception.ConflictBusinessRuleException;
import co.edu.uco.seguridad.pdp.profiles.domain.message.ProfilesMessages;
import co.edu.uco.seguridad.pdp.profiles.domain.model.ProfileName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;

/**
 * Ya hay un perfil con ese nombre en ese alcance exacto (nivel + inquilino + aplicación).
 */
public final class DuplicateProfileNameException extends ConflictBusinessRuleException {

    public DuplicateProfileNameException(ProfileName name, RoleScope scope) {
        super("PROFILE_NAME_TAKEN", ProfilesMessages.profileNameTaken(name.value(), scope.level().name()));
    }
}
