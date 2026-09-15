package co.edu.uco.seguridad.pdp.applications.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * DTO de entrada del puerto primario: la aplicación que dice ser quien dice ser, y el secreto que
 * lo demuestra. {@code secret} es texto libre en tránsito, sin invariante de dominio propio (HU-013).
 */
public record ValidateApplicationCredentialRequest(ApplicationName applicationName, String secret) {

    public ValidateApplicationCredentialRequest {
        Objects.requireNonNull(applicationName, RequiredArgumentMessages.APPLICATION_NAME);
        Objects.requireNonNull(secret, RequiredArgumentMessages.APPLICATION_CREDENTIAL);
    }
}
