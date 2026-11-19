package co.edu.uco.seguridad.pdp.resources.application.usecase;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.response.RegisteredProtectedResourceResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveStreamOperation;

/**
 * Puerto primario: listar los endpoints protegidos registrados bajo una aplicación.
 */
public interface ListProtectedResourcesUseCase
        extends ReactiveStreamOperation<ApplicationId, RegisteredProtectedResourceResponse> {
}
