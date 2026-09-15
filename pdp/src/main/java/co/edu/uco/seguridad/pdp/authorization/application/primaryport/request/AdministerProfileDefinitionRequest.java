package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.DefineProfileRequest;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;
import java.util.Optional;

/**
 * Entrada de {@code AdministerProfileDefinitionUseCase} (HU-019): la solicitud de definición de
 * perfil, más la administración a gatear si el perfil es de alcance {@code APPLICATION}.
 * {@code Optional} vacío significa "esta escritura no requiere administración" (perfil
 * {@code TENANT}) — mismo criterio que {@code AdministerRoleDefinitionRequest} (HU-016).
 */
public record AdministerProfileDefinitionRequest(Optional<AdministrationRequest> administration, DefineProfileRequest profile) {

    public AdministerProfileDefinitionRequest {
        Objects.requireNonNull(administration, RequiredArgumentMessages.ADMINISTRATION);
        Objects.requireNonNull(profile, RequiredArgumentMessages.DEFINE_PROFILE_REQUEST);
    }
}
