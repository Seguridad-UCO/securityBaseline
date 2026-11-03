package co.edu.uco.seguridad.pdp.applications.domain.model;

import co.edu.uco.seguridad.pdp.applications.domain.exception.InvalidApplicationCredentialHashException;
import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;

/**
 * El hash del secreto de una aplicación, nunca el secreto en claro (HU-012). Sin restricción de
 * formato: lo produce {@code CredentialHasher}, no una persona, así que la única invariante es que
 * exista.
 */
public record ApplicationCredentialHash(String value) {

    public ApplicationCredentialHash {
        if (value == null) {
            throw new InvalidApplicationCredentialHashException(ValueObjectMessages.VALUE_REQUIRED);
        }
        value = value.trim();
        if (value.isEmpty()) {
            throw new InvalidApplicationCredentialHashException(ValueObjectMessages.VALUE_REQUIRED);
        }
    }
}
