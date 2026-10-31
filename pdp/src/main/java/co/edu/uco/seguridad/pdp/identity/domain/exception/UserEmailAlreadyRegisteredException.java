package co.edu.uco.seguridad.pdp.identity.domain.exception;

import co.edu.uco.seguridad.pdp.commons.exception.ConflictBusinessRuleException;
import co.edu.uco.seguridad.pdp.identity.domain.message.IdentityMessages;
import co.edu.uco.seguridad.pdp.identity.domain.model.Email;

/**
 * El correo con que se iba a crear un usuario ya pertenece a otro (HU-038). En el canal Bearer se
 * traduce a 401 uniforme; si algún día se expone por HTTP de negocio, es un conflicto (409).
 */
public final class UserEmailAlreadyRegisteredException extends ConflictBusinessRuleException {

    public UserEmailAlreadyRegisteredException(Email email) {
        super("USER_EMAIL_ALREADY_REGISTERED", IdentityMessages.userEmailAlreadyRegistered(email.value()));
    }
}
