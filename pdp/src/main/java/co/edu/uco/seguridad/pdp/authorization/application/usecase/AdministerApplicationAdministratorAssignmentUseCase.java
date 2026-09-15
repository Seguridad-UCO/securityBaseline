package co.edu.uco.seguridad.pdp.authorization.application.usecase;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerApplicationAdministratorAssignmentRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/** Gatea el alta de un administrador de aplicación tras HU-009 (HU-020). */
public interface AdministerApplicationAdministratorAssignmentUseCase
        extends ReactiveOperation<AdministerApplicationAdministratorAssignmentRequest, AssignmentResponse> {
}
