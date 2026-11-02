package co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.request;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;

import java.util.Objects;
import java.util.Set;

public record CreateServiceTechnicalRoleRequest(AdministrationRequest administration, String code, String name,
                                                Set<ResourceId> resourceIds) {
    public CreateServiceTechnicalRoleRequest {
        Objects.requireNonNull(administration);
        Objects.requireNonNull(code);
        Objects.requireNonNull(name);
        Objects.requireNonNull(resourceIds);
    }
}
