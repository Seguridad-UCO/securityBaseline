package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ListApplicationAdministratorsRequest;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

public record AdministerApplicationAdministratorListRequest(
        AdministrationRequest administration, ListApplicationAdministratorsRequest query) {

    public AdministerApplicationAdministratorListRequest {
        Objects.requireNonNull(administration, RequiredArgumentMessages.ADMINISTRATION_REQUEST);
        Objects.requireNonNull(query, RequiredArgumentMessages.LIST_APPLICATION_ADMINISTRATORS_REQUEST);
    }
}
