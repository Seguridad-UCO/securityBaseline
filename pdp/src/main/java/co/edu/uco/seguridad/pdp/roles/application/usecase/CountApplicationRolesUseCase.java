package co.edu.uco.seguridad.pdp.roles.application.usecase;

import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.ApplicationRoleCountRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface CountApplicationRolesUseCase extends ReactiveOperation<ApplicationRoleCountRequest, Long> {
}
