package co.edu.uco.seguridad.pdp.tenants.application.rule;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

/** Regla con repositorio: el id de un tenant es único globalmente (no hay jerarquías de tenants). */
public interface TenantCodeMustBeUniqueRule extends ReactiveOperationWithoutResult<TenantId> {
}
