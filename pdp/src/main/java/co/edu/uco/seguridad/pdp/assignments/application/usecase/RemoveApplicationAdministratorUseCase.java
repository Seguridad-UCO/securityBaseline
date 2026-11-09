package co.edu.uco.seguridad.pdp.assignments.application.usecase;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RemoveApplicationAdministratorRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

/**
 * Revoca la asignación del rol {@code ADMIN} de un usuario en una aplicación, rechazando la
 * operación si es el único administrador activo (HU-020, {@code LastAdministratorMustNotBeRevokedRule}).
 */
public interface RemoveApplicationAdministratorUseCase
        extends ReactiveOperationWithoutResult<RemoveApplicationAdministratorRequest> {
}
