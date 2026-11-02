package co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.request;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;

import java.util.Objects;
import java.util.UUID;

public record ChangeServiceTechnicalRoleResourceRequest(AdministrationRequest administration, UUID technicalRoleId,
                                                        ResourceId resourceId) {
    public ChangeServiceTechnicalRoleResourceRequest {
        Objects.requireNonNull(administration);
        Objects.requireNonNull(technicalRoleId);
        Objects.requireNonNull(resourceId);
    }
}
