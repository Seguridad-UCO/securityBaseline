package co.edu.uco.seguridad.pdp.roles.application.usecase;

import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.UpdateRoleRequest;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;
public interface UpdateRoleUseCase extends ReactiveOperation<UpdateRoleRequest, RoleResponse> { }
