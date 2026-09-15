package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RemoveApplicationAdministratorRequest;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

public record AdministerApplicationAdministratorRemovalRequest(
        AdministrationRequest administration, RemoveApplicationAdministratorRequest removal) {

    public AdministerApplicationAdministratorRemovalRequest {
        Objects.requireNonNull(administration, RequiredArgumentMessages.ADMINISTRATION_REQUEST);
        Objects.requireNonNull(removal, RequiredArgumentMessages.REMOVE_APPLICATION_ADMINISTRATOR_REQUEST);
    }
}
