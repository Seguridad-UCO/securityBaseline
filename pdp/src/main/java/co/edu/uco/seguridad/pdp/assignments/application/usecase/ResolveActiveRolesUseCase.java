package co.edu.uco.seguridad.pdp.assignments.application.usecase;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ResolveActiveRolesRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ActiveRolesResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Sin interactor ni controller en esta historia: lo invocará HU-006 en proceso.
 */
public interface ResolveActiveRolesUseCase extends ReactiveOperation<ResolveActiveRolesRequest, ActiveRolesResponse> {
}
