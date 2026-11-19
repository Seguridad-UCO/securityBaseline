package co.edu.uco.seguridad.pdp.authorization.application.usecase;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

/**
 * Gatea {@code RemoveApplicationUseCase} (HU-015): valida con
 * {@code PrincipalMustBeApplicationAdministratorValidator} y, si permite, delega en
 * {@code applications :: usecase :: RemoveApplicationUseCase}. Vive en {@code authorization} porque
 * ya depende de {@code applications} — {@code applications} no puede depender de vuelta sin crear un
 * ciclo (ver PLAN-HU-015.md §0).
 */
public interface AdministerApplicationRemovalUseCase extends ReactiveOperationWithoutResult<AdministrationRequest> {
}
