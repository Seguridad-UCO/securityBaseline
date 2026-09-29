package co.edu.uco.seguridad.pdp.roles.application.usecase;

import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.ListApplicationRolesPageRequest;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface ListApplicationRolesPageUseCase extends ReactiveOperation<ListApplicationRolesPageRequest, ResultPage<RoleResponse>> {
}
