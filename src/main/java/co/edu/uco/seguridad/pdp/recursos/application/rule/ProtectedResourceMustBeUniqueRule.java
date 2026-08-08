package co.edu.uco.seguridad.pdp.recursos.application.rule;

import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

/**
 * Regla con repositorio: la tripleta (aplicación, código de recurso, acción) puede otorgarse una sola vez.
 */
public interface ProtectedResourceMustBeUniqueRule extends ReactiveOperationWithoutResult<ProtectedResourceRegistration> {
}
