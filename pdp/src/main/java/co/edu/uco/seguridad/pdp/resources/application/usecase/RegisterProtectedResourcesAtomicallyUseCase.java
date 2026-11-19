package co.edu.uco.seguridad.pdp.resources.application.usecase;

import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.RegisterProtectedResourcesAtomicallyRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

public interface RegisterProtectedResourcesAtomicallyUseCase
        extends ReactiveOperationWithoutResult<RegisterProtectedResourcesAtomicallyRequest> {
}
