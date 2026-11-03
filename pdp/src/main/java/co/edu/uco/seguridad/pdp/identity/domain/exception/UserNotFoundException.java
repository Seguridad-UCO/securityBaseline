package co.edu.uco.seguridad.pdp.identity.domain.exception;

import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.domain.message.IdentityMessages;

public final class UserNotFoundException extends BusinessRuleViolationException {

    public UserNotFoundException(UserId userId) {
        super("USER_NOT_FOUND", IdentityMessages.userNotFound(userId.value().toString()));
    }
}
