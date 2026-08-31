package co.edu.uco.seguridad.pdp.identity.application.usecase;

import co.edu.uco.seguridad.pdp.identity.application.primaryport.request.ProvisionIdentityRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;
import co.edu.uco.seguridad.shared.security.LocalUserPrincipal;

/**
 * Puerto primario: reconocer o crear el usuario propio correspondiente a una identidad externa ya
 * autenticada por Keycloak.
 */
public interface ProvisionIdentityUseCase extends ReactiveOperation<ProvisionIdentityRequest, LocalUserPrincipal> {
}
