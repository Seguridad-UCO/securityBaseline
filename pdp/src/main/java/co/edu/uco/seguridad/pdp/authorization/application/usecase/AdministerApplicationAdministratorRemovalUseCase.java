package co.edu.uco.seguridad.pdp.authorization.application.usecase;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerApplicationAdministratorRemovalRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

/** Gatea la remoción de un administrador de aplicación tras HU-009 (HU-020). */
public interface AdministerApplicationAdministratorRemovalUseCase
        extends ReactiveOperationWithoutResult<AdministerApplicationAdministratorRemovalRequest> {
}
