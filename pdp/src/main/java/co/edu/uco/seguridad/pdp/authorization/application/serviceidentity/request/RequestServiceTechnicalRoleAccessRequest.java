package co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.request;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceIdentityId;

import java.util.Objects;
import java.util.UUID;

public record RequestServiceTechnicalRoleAccessRequest(AdministrationRequest administration,
                                                       ServiceIdentityId serviceIdentityId,
                                                       UUID technicalRoleId) {
    public RequestServiceTechnicalRoleAccessRequest {
        Objects.requireNonNull(administration);
        Objects.requireNonNull(serviceIdentityId);
        Objects.requireNonNull(technicalRoleId);
    }
}
