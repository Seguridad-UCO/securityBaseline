package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.GrantResourceRequest;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entrada de {@code AdministerResourceGrantUseCase} (HU-016, HU-036): la solicitud de concesión o revocación
 * de recurso, más el gate que exige el alcance del rol que la recibe. Ver {@link CatalogAdministration}.
 */
public record AdministerResourceGrantRequest(CatalogAdministration administration, GrantResourceRequest grant,
                                             boolean revocation) {

    public AdministerResourceGrantRequest {
        Objects.requireNonNull(administration, RequiredArgumentMessages.CATALOG_ADMINISTRATION);
        Objects.requireNonNull(grant, RequiredArgumentMessages.GRANT_RESOURCE_REQUEST);
    }

    public AdministerResourceGrantRequest(CatalogAdministration administration, GrantResourceRequest grant) {
        this(administration, grant, false);
    }
}
