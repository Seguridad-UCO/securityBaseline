package co.edu.uco.seguridad.pdp.tenants.domain.rule;

import co.edu.uco.seguridad.pdp.tenants.domain.rule.model.TenantActivation;
import co.edu.uco.seguridad.shared.contract.OperationWithoutResult;

/**
 * El inquilino ya existente tiene que estar en un estado que admita operar.
 *
 * <p>La existencia no es asunto suyo: eso lo decide {@link TenantMustExistRule} antes.</p>
 */
public interface TenantStatusMustBeActiveRule extends OperationWithoutResult<TenantActivation> {
}
