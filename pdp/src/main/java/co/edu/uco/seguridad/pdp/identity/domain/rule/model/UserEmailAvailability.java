package co.edu.uco.seguridad.pdp.identity.domain.rule.model;

import co.edu.uco.seguridad.pdp.identity.domain.model.Email;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entrada ya resuelta de {@code UserEmailMustBeAvailableRule} (HU-038): el correo consultado y si el
 * almacén ya lo tenía asignado a algún usuario.
 */
public record UserEmailAvailability(Email email, boolean taken) {

    public UserEmailAvailability {
        Objects.requireNonNull(email, RequiredArgumentMessages.USER_EMAIL);
    }
}
