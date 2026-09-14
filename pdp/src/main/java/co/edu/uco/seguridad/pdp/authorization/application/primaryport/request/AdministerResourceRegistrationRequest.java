package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.RegisterProtectedResourceRequest;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entrada de {@code AdministerResourceRegistrationUseCase} (HU-017): la solicitud de registro de
 * recurso, más la administración a gatear. A diferencia de {@code AdministerRoleDefinitionRequest}
 * (HU-016), {@code administration} no es {@code Optional}: un recurso protegido siempre pertenece a
 * una aplicación, así que el gate siempre se evalúa.
 */
public record AdministerResourceRegistrationRequest(AdministrationRequest administration,
        RegisterProtectedResourceRequest resource) {

    public AdministerResourceRegistrationRequest {
        Objects.requireNonNull(administration, RequiredArgumentMessages.ADMINISTRATION_REQUEST);
        Objects.requireNonNull(resource, RequiredArgumentMessages.REGISTER_PROTECTED_RESOURCE_REQUEST);
    }
}
