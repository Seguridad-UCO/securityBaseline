package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignRoleRequest;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entrada de {@code AdministerAssignmentCreationUseCase} (HU-018): la solicitud de asignación de
 * rol, más la administración a gatear. Sin {@code Optional} — {@code AssignRoleRequest.applicationId()}
 * siempre está presente, así que el gate siempre se evalúa (mismo criterio que
 * {@code AdministerResourceRegistrationRequest}, HU-017).
 */
public record AdministerAssignmentCreationRequest(AdministrationRequest administration, AssignRoleRequest assignment) {

    public AdministerAssignmentCreationRequest {
        Objects.requireNonNull(administration, RequiredArgumentMessages.ADMINISTRATION_REQUEST);
        Objects.requireNonNull(assignment, RequiredArgumentMessages.ASSIGN_ROLE_REQUEST);
    }
}
