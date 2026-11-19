package co.edu.uco.seguridad.pdp.assignments.domain.rule;

import co.edu.uco.seguridad.pdp.assignments.domain.rule.model.AdministratorRevocationEligibility;
import co.edu.uco.seguridad.shared.contract.OperationWithoutResult;

/**
 * Rechaza revocar la asignación del rol {@code ADMIN} cuando es la única activa de esa aplicación
 * (HU-020). Pura y síncrona: recibe el conteo ya resuelto, no consulta nada.
 */
public interface LastAdministratorMustNotBeRevokedRule
        extends OperationWithoutResult<AdministratorRevocationEligibility> {
}
