package co.edu.uco.seguridad.pdp.assignments.application.usecase;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ListApplicationAdministratorsRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

import java.util.List;

/** Lista los administradores activos (asignaciones del rol {@code ADMIN}) de una aplicación (HU-020). */
public interface ListApplicationAdministratorsUseCase
        extends ReactiveOperation<ListApplicationAdministratorsRequest, List<AssignmentResponse>> {
}
