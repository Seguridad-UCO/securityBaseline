package co.edu.uco.seguridad.pdp.tenants.application.rule;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.tenants.application.port.primary.dto.response.TenantResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Regla publicada: un inquilino debe existir y estar activo antes de poder actuar.
 *
 * <p>Respaldada por repositorio, por lo que es reactiva. Devuelve el DTO que tuvo que cargar, ahorrando a cada
 * llamador una segunda búsqueda. Los consumidores inyectan este contrato en lugar de reimplementar la verificación,
 * lo que es por qué la decisión no puede desviarse entre módulos.</p>
 */
public interface TenantMustBeActiveRule extends ReactiveOperation<TenantId, TenantResponse> {
}
