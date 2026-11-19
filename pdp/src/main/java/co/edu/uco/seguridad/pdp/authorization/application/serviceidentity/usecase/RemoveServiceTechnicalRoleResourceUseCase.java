package co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.usecase;

import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.request.ChangeServiceTechnicalRoleResourceRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface RemoveServiceTechnicalRoleResourceUseCase extends ReactiveOperation<ChangeServiceTechnicalRoleResourceRequest, Void> {
}
