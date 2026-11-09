package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignApplicationAdministratorRequest;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

public record AdministerApplicationAdministratorAssignmentRequest(
        AdministrationRequest administration, AssignApplicationAdministratorRequest assignment) {

    public AdministerApplicationAdministratorAssignmentRequest {
        Objects.requireNonNull(administration, RequiredArgumentMessages.ADMINISTRATION_REQUEST);
        Objects.requireNonNull(assignment, RequiredArgumentMessages.ASSIGN_APPLICATION_ADMINISTRATOR_REQUEST);
    }
}
