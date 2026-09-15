package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.AddRoleToProfileRequest;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;
import java.util.Optional;

/**
 * Entrada de {@code AdministerProfileRoleAdditionUseCase} (HU-019): la solicitud de adición de rol
 * a un perfil, más la administración a gatear (resuelta vía {@code ProfileApplicationLookupValidator}).
 * {@code Optional} vacío para perfil {@code TENANT} — mismo criterio que
 * {@code AdministerResourceGrantRequest} (HU-016).
 */
public record AdministerProfileRoleAdditionRequest(Optional<AdministrationRequest> administration, AddRoleToProfileRequest addition) {

    public AdministerProfileRoleAdditionRequest {
        Objects.requireNonNull(administration, RequiredArgumentMessages.ADMINISTRATION);
        Objects.requireNonNull(addition, RequiredArgumentMessages.ADD_ROLE_TO_PROFILE_REQUEST);
    }
}
