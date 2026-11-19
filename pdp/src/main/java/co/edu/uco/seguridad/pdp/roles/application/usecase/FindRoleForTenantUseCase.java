package co.edu.uco.seguridad.pdp.roles.application.usecase;

import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.FindRoleForTenantRequest;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface FindRoleForTenantUseCase extends ReactiveOperation<FindRoleForTenantRequest, RoleResponse> {
}
