package co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.request.raw.DefineRoleRawRequest;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.response.RoleWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface DefineRoleInteractor extends ReactiveOperation<DefineRoleRawRequest, RoleWebResponse> {
}
