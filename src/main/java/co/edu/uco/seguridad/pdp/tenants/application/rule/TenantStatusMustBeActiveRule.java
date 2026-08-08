package co.edu.uco.seguridad.pdp.tenants.application.rule;

import co.edu.uco.seguridad.pdp.tenants.domain.Tenant;
import co.edu.uco.seguridad.shared.contract.OperationWithoutResult;

/**
 * Regla sincrónica pura: el inquilino ya cargado debe estar activo.
 *
 * <p>No toca repositorio. La carga y la existencia las resuelve
 * {@link TenantMustBeActiveRule}; esta regla solo decide sobre el estado.</p>
 */
public interface TenantStatusMustBeActiveRule extends OperationWithoutResult<Tenant> {
}
