package co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.request;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;

import java.util.Objects;
import java.util.UUID;

/**
 * Revocación auditable de un permiso M2M ya aprobado por la aplicación destino.
 */
public record RevokeServiceAccessRequest(AdministrationRequest administration, UUID grantId) {
    public RevokeServiceAccessRequest {
        Objects.requireNonNull(administration);
        Objects.requireNonNull(grantId);
    }
}
