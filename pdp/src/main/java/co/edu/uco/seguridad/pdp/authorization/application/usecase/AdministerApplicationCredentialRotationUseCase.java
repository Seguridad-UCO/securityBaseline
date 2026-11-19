package co.edu.uco.seguridad.pdp.authorization.application.usecase;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.ApplicationRegistrationResponse;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Gatea {@code RotateApplicationCredentialUseCase} (HU-015): mismo patrón que
 * {@code AdministerApplicationRemovalUseCase}, pero devuelve la credencial nueva en vez de
 * {@code Void}.
 */
public interface AdministerApplicationCredentialRotationUseCase
        extends ReactiveOperation<AdministrationRequest, ApplicationRegistrationResponse> {
}
