package co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.request;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;

import java.util.Objects;

public record CreateServiceIdentityRequest(AdministrationRequest administration, String name) {
    public CreateServiceIdentityRequest {
        Objects.requireNonNull(administration);
        Objects.requireNonNull(name);
    }
}
