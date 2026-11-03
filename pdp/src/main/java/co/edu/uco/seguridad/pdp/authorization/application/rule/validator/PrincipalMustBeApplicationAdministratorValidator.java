package co.edu.uco.seguridad.pdp.authorization.application.rule.validator;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

/**
 * Contrato publicado a los demás módulos (HU-009): el sujeto administra la aplicación indicada, o la
 * cadena termina en {@code NotAuthorizedToAdministerException}. Mismo patrón que
 * {@code ApplicationMustExistForTenantValidator} de {@code applications} — una única implementación
 * inyectada, no una comprobación copiada por cada slice que la necesite.
 */
public interface PrincipalMustBeApplicationAdministratorValidator
        extends ReactiveOperationWithoutResult<AdministrationRequest> {
}
