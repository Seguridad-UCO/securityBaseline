package co.edu.uco.seguridad.pdp.identity.application.exception;

import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;
import co.edu.uco.seguridad.pdp.identity.application.message.IdentityMessages;
import co.edu.uco.seguridad.pdp.identity.domain.UserId;

public final class UserNotFoundException extends BusinessRuleViolationException {

    public UserNotFoundException(UserId userId) {
        super("USER_NOT_FOUND", IdentityMessages.userNotFound(userId.value().toString()));
    }
}
