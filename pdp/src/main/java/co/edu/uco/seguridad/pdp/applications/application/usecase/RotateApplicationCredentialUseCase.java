package co.edu.uco.seguridad.pdp.applications.application.usecase;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.RotateApplicationCredentialRequest;
import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.ApplicationRegistrationResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Puerto primario: rotar la credencial de una aplicación ya registrada (HU-014). Devuelve la misma
 * forma que el registro (HU-012) — el secreto nuevo en texto plano, una sola vez.
 */
public interface RotateApplicationCredentialUseCase
        extends ReactiveOperation<RotateApplicationCredentialRequest, ApplicationRegistrationResponse> {
}
