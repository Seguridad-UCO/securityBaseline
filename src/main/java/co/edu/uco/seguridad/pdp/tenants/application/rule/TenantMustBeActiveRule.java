package co.edu.uco.seguridad.pdp.tenants.application.rule;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.tenants.application.port.primary.dto.response.TenantResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Regla publicada: un inquilino debe existir y estar activo antes de poder actuar.
 *
 * <p>Respaldada por repositorio (reactiva). Carga el inquilino y delega el chequeo de estado a
 * {@link TenantStatusMustBeActiveRule}. Devuelve el DTO cargado para evitar una segunda búsqueda.
 * Los consumidores inyectan este contrato; la decisión no puede desviarse entre módulos.</p>
 */
public interface TenantMustBeActiveRule extends ReactiveOperation<TenantId, TenantResponse> {
}
