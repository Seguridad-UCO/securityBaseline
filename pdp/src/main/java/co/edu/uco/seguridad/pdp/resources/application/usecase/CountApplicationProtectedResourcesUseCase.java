package co.edu.uco.seguridad.pdp.resources.application.usecase;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface CountApplicationProtectedResourcesUseCase extends ReactiveOperation<ApplicationId, Long> {
}
