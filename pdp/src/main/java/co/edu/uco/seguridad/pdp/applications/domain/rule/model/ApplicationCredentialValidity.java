package co.edu.uco.seguridad.pdp.applications.domain.rule.model;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/** Hecho ya resuelto para {@link co.edu.uco.seguridad.pdp.applications.domain.rule.ApplicationCredentialMustBeValidRule}. */
public record ApplicationCredentialValidity(ApplicationId applicationId, boolean valid) {

    public ApplicationCredentialValidity {
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
    }
}
