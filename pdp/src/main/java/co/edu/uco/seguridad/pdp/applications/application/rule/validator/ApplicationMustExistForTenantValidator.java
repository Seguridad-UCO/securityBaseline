package co.edu.uco.seguridad.pdp.applications.application.rule.validator;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ApplicationOwnershipQuery;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

/**
 * Contrato publicado a los demás módulos: la aplicación existe y pertenece al inquilino indicado.
 *
 * <p>Lo consume {@code resources}, igual que {@code tenants} publica
 * {@code TenantMustBeActiveValidator}: una única implementación inyectada, no una comprobación
 * copiada, para que la decisión no pueda divergir entre módulos.</p>
 *
 * <p>No devuelve la aplicación: su único consumidor descartaba el agregado, y devolverlo obligaba
 * al puerto a cargar la fila entera para responder si existe.</p>
 */
public interface ApplicationMustExistForTenantValidator
        extends ReactiveOperationWithoutResult<ApplicationOwnershipQuery> {
}
