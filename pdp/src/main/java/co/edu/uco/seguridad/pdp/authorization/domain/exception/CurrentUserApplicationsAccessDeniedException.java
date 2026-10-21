package co.edu.uco.seguridad.pdp.authorization.domain.exception;

import co.edu.uco.seguridad.pdp.authorization.domain.message.AuthorizationMessages;
import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;

/** La vista del sujeto actual no revela datos cuando una referencia local no es consistente. */
public final class CurrentUserApplicationsAccessDeniedException extends BusinessRuleViolationException {

    public CurrentUserApplicationsAccessDeniedException() {
        super("CURRENT_USER_APPLICATIONS_ACCESS_DENIED", AuthorizationMessages.currentUserApplicationsAccessDenied());
    }
}
