package co.edu.uco.seguridad.pdp.applications.application.usecase;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ValidateApplicationCredentialRequest;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Puerto primario: validar la credencial de una aplicación (HU-013). Devuelve el {@code TenantId}
 * dueño si es válida; rechaza con {@code InvalidApplicationCredentialException} en cualquier otro
 * caso, sin distinguir la causa.
 */
public interface ValidateApplicationCredentialUseCase
        extends ReactiveOperation<ValidateApplicationCredentialRequest, TenantId> {
}
