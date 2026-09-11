package co.edu.uco.seguridad.pdp.tenants.domain.rule;

import co.edu.uco.seguridad.pdp.tenants.domain.rule.model.TenantExistence;
import co.edu.uco.seguridad.shared.contract.OperationWithoutResult;

/** El inquilino referenciado tiene que estar registrado. */
public interface TenantMustExistRule extends OperationWithoutResult<TenantExistence> {
}
