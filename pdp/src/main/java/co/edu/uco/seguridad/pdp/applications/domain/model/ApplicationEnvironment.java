package co.edu.uco.seguridad.pdp.applications.domain.model;

import co.edu.uco.seguridad.pdp.applications.domain.exception.InvalidApplicationEnvironmentException;

import java.util.Locale;

/** Ambientes soportados para desplegar una aplicación registrada. */
public enum ApplicationEnvironment {
    DEV, LAB, PROD;

    public static ApplicationEnvironment from(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidApplicationEnvironmentException();
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidApplicationEnvironmentException();
        }
    }

    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }
}
