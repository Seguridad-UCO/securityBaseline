package co.edu.uco.seguridad.pdp.applications.application.rule;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

/**
 * Regla con repositorio: un nombre de aplicación es único dentro de un inquilino, no globalmente.
 *
 * <p>Toma el DTO completo porque la regla es sobre el par, no sobre ningún valor solo.</p>
 */
public interface ApplicationNameMustBeUniqueForTenantRule extends ReactiveOperationWithoutResult<RegisterApplicationRequest> {
}
