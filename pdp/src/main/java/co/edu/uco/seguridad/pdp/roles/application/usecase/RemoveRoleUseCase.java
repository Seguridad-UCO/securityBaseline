package co.edu.uco.seguridad.pdp.roles.application.usecase;

import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;
public interface RemoveRoleUseCase extends ReactiveOperationWithoutResult<RemoveRoleUseCase.Request> {
    record Request(TenantId tenantId, RoleId roleId) { }
}
