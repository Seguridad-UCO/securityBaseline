package co.edu.uco.seguridad.pdp.roles.domain.model;

import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;
import co.edu.uco.seguridad.pdp.roles.domain.exception.InvalidRoleNameException;

/**
 * Nombre de un rol: 3 a 60 caracteres tras normalizar. Su unicidad dentro del alcance es una regla, no un invariante.
 */
public record RoleName(String value) {

    public RoleName {
        if (value == null) {
            throw new InvalidRoleNameException(ValueObjectMessages.VALUE_REQUIRED);
        }
        value = value.trim();
        if (value.isEmpty() || value.length() < 3 || value.length() > 60) {
            throw new InvalidRoleNameException(ValueObjectMessages.RoleName.LENGTH);
        }
    }
}
