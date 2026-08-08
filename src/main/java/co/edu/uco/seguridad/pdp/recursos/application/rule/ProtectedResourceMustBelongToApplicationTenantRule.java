package co.edu.uco.seguridad.pdp.recursos.application.rule;

import co.edu.uco.seguridad.shared.contract.OperationWithoutResult;

/**
 * Regla sin repositorio: el inquilino del recurso y el inquilino de su aplicación deben ser el mismo.
 *
 * <p>Ambos valores ya están disponibles, por lo que la verificación no necesita E/S y permanece sincrónica.</p>
 */
public interface ProtectedResourceMustBelongToApplicationTenantRule extends OperationWithoutResult<ProtectedResourceRegistration> {
}
