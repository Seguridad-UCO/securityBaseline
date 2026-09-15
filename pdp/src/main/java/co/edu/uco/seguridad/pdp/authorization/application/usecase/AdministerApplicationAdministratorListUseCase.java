package co.edu.uco.seguridad.pdp.authorization.application.usecase;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerApplicationAdministratorListRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

import java.util.List;

/** Gatea el listado de administradores de aplicación tras HU-009 (HU-020). */
public interface AdministerApplicationAdministratorListUseCase
        extends ReactiveOperation<AdministerApplicationAdministratorListRequest, List<AssignmentResponse>> {
}
