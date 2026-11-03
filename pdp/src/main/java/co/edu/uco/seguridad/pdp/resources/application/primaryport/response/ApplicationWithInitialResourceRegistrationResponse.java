package co.edu.uco.seguridad.pdp.resources.application.primaryport.response;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.ApplicationRegistrationResponse;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * DTO de salida del puerto primario (HU-010): la aplicación registrada (con su credencial en claro,
 * HU-012) más su recurso inicial. Combina dos proyecciones ya existentes, sin duplicar campos.
 */
public record ApplicationWithInitialResourceRegistrationResponse(ApplicationRegistrationResponse application,
                                                                 RegisteredProtectedResourceResponse resource) {

    public ApplicationWithInitialResourceRegistrationResponse {
        Objects.requireNonNull(application, RequiredArgumentMessages.REGISTERED_APPLICATION_RESPONSE);
        Objects.requireNonNull(resource, RequiredArgumentMessages.REGISTERED_PROTECTED_RESOURCE_RESPONSE);
    }
}
