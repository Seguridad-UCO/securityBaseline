package co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.request;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import java.util.Objects;
import java.util.UUID;

public record DecideServiceAccessRequest(AdministrationRequest administration, UUID grantId, boolean approve) {
    public DecideServiceAccessRequest { Objects.requireNonNull(administration); Objects.requireNonNull(grantId); }
}
