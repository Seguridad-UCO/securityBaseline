package co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.request;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceIdentityId;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;

import java.util.Objects;

public record RequestServiceAccessRequest(AdministrationRequest administration, ServiceIdentityId serviceIdentityId,
                                          ResourceId resourceId, String technicalRole) {
    public RequestServiceAccessRequest {
        Objects.requireNonNull(administration);
        Objects.requireNonNull(serviceIdentityId);
        Objects.requireNonNull(resourceId);
        Objects.requireNonNull(technicalRole);
    }
}
