package co.edu.uco.seguridad.pdp.applications.application.primaryport.response;

import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * DTO de salida exclusivo de {@code RegisterApplicationUseCase}: la proyección habitual de la
 * aplicación más el secreto en texto plano, que solo existe en este instante (HU-012). Nunca se
 * reconstruye desde persistencia — {@code ListApplicationsUseCase} sigue devolviendo
 * {@link RegisteredApplicationResponse} sin este campo.
 */
public record ApplicationRegistrationResponse(RegisteredApplicationResponse application, String credential) {

    public ApplicationRegistrationResponse {
        Objects.requireNonNull(application, RequiredArgumentMessages.REGISTERED_APPLICATION_RESPONSE);
        Objects.requireNonNull(credential, RequiredArgumentMessages.APPLICATION_CREDENTIAL);
    }
}
