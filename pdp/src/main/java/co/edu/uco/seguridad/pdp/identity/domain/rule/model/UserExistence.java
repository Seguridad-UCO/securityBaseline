package co.edu.uco.seguridad.pdp.identity.domain.rule.model;

import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entrada ya resuelta de {@code UserMustExistRule}: qué usuario se preguntó y si el almacén lo tenía.
 */
public record UserExistence(UserId userId, boolean registered) {

    public UserExistence {
        Objects.requireNonNull(userId, RequiredArgumentMessages.USER_ID);
    }
}
