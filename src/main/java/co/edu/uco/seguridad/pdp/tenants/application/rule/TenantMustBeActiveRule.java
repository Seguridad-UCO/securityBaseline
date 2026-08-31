package co.edu.uco.seguridad.pdp.tenants.application.rule;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.tenants.application.primaryport.response.TenantResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Regla publicada: un inquilino debe existir y estar activo antes de poder actuar. Carga el
 * inquilino y delega el chequeo de estado a {@link TenantStatusMustBeActiveRule}.
 */
public interface TenantMustBeActiveRule extends ReactiveOperation<TenantId, TenantResponse> {
}
