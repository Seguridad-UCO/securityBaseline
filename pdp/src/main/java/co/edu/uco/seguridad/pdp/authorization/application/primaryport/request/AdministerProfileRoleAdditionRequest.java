package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.AddRoleToProfileRequest;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entrada de {@code AdministerProfileRoleAdditionUseCase} (HU-019, HU-036): la solicitud de alta o baja de
 * un rol en un perfil, más el gate que exige el alcance del perfil. Ver {@link CatalogAdministration}.
 */
public record AdministerProfileRoleAdditionRequest(CatalogAdministration administration,
                                                   AddRoleToProfileRequest addition,
                                                   boolean removal) {

    public AdministerProfileRoleAdditionRequest {
        Objects.requireNonNull(administration, RequiredArgumentMessages.CATALOG_ADMINISTRATION);
        Objects.requireNonNull(addition, RequiredArgumentMessages.ADD_ROLE_TO_PROFILE_REQUEST);
    }

    public AdministerProfileRoleAdditionRequest(CatalogAdministration administration, AddRoleToProfileRequest addition) {
        this(administration, addition, false);
    }
}
