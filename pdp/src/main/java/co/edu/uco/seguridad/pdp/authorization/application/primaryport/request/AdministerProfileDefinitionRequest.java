package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.DefineProfileRequest;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entrada de {@code AdministerProfileDefinitionUseCase} (HU-019, HU-036): la solicitud de definición de
 * perfil, más el gate que exige su alcance (aplicación o inquilino). Ver {@link CatalogAdministration}.
 */
public record AdministerProfileDefinitionRequest(CatalogAdministration administration,
                                                 DefineProfileRequest profile) {

    public AdministerProfileDefinitionRequest {
        Objects.requireNonNull(administration, RequiredArgumentMessages.CATALOG_ADMINISTRATION);
        Objects.requireNonNull(profile, RequiredArgumentMessages.DEFINE_PROFILE_REQUEST);
    }
}
