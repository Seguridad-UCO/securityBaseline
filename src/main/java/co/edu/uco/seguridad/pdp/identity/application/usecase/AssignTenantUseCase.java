package co.edu.uco.seguridad.pdp.identity.application.usecase;

import co.edu.uco.seguridad.pdp.identity.application.primaryport.request.AssignTenantRequest;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.response.UserResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/** Puerto primario: reasignar el tenant de un usuario existente a un tenant activo existente. */
public interface AssignTenantUseCase extends ReactiveOperation<AssignTenantRequest, UserResponse> {
}
