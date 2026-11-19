package co.edu.uco.seguridad.pdp.tenants.domain.rule;

import co.edu.uco.seguridad.pdp.tenants.domain.rule.model.TenantCodeAvailability;
import co.edu.uco.seguridad.shared.contract.OperationWithoutResult;

/**
 * El id de un inquilino es único globalmente: no hay jerarquías de inquilinos.
 */
public interface TenantCodeMustBeUniqueRule extends OperationWithoutResult<TenantCodeAvailability> {
}
