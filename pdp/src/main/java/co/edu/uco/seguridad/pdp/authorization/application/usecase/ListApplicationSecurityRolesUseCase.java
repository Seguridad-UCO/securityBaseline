package co.edu.uco.seguridad.pdp.authorization.application.usecase;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.ListApplicationSecurityRolesRequest;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;
public interface ListApplicationSecurityRolesUseCase extends ReactiveOperation<ListApplicationSecurityRolesRequest, ResultPage<RoleResponse>> { }
