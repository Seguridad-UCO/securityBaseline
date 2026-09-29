package co.edu.uco.seguridad.pdp.resources.application.usecase;

import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

public interface RemoveProtectedResourceUseCase extends ReactiveOperationWithoutResult<RemoveProtectedResourceUseCase.Request> {
    record Request(TenantId tenantId, ResourceId resourceId) { }
}
