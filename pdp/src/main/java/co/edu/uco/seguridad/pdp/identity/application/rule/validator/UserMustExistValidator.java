package co.edu.uco.seguridad.pdp.identity.application.rule.validator;

import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

/**
 * Contrato publicado a los demás módulos: un usuario existe.
 *
 * <p>Es un validador y no la regla desnuda porque hace E/S: resuelve la existencia contra el puerto
 * y deja decidir a {@link co.edu.uco.seguridad.pdp.identity.domain.rule.UserMustExistRule}. Hasta
 * ahora {@code UserMustExistRule} solo la usaba {@code AssignTenantUseCaseImpl} como su propio
 * validador (un único consumidor interno); {@code assignments} es el primer consumidor externo, y
 * por eso hace falta publicarla (HU-005).</p>
 */
public interface UserMustExistValidator extends ReactiveOperationWithoutResult<UserId> {
}
