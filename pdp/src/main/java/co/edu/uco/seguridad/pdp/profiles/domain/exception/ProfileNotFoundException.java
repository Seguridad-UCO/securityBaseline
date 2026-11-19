package co.edu.uco.seguridad.pdp.profiles.domain.exception;

import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.profiles.domain.message.ProfilesMessages;

/**
 * El perfil no existe, o no es de este inquilino: un perfil ajeno o global no existe para quien pregunta.
 */
public final class ProfileNotFoundException extends BusinessRuleViolationException {

    public ProfileNotFoundException(ProfileId profileId) {
        super("PROFILE_NOT_FOUND", ProfilesMessages.profileNotFound(profileId.value().toString()));
    }
}
