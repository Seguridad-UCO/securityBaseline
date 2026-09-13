package co.edu.uco.seguridad.pdp.profiles.domain.model;

import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;
import co.edu.uco.seguridad.pdp.profiles.domain.exception.InvalidProfileNameException;

/** Nombre de un perfil: 3 a 60 caracteres tras normalizar. Su unicidad dentro del alcance es una regla, no un invariante. */
public record ProfileName(String value) {

    public ProfileName {
        if (value == null) {
            throw new InvalidProfileNameException(ValueObjectMessages.VALUE_REQUIRED);
        }
        value = value.trim();
        if (value.isEmpty() || value.length() < 3 || value.length() > 60) {
            throw new InvalidProfileNameException(ValueObjectMessages.ProfileName.LENGTH);
        }
    }
}
