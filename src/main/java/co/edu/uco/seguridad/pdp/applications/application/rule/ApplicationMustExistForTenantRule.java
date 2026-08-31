package co.edu.uco.seguridad.pdp.applications.application.rule;

import co.edu.uco.seguridad.pdp.applications.application.port.primary.dto.request.ApplicationOwnershipQuery;
import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * La aplicación existe y pertenece al inquilino indicado.
 *
 * <p>La publica {@code applications} y la consume {@code resources}, igual que {@code tenants}
 * publica {@code TenantMustBeActiveRule}: una única implementación inyectada, no una comprobación
 * copiada, para que la decisión no pueda divergir entre módulos.
 */
public interface ApplicationMustExistForTenantRule extends ReactiveOperation<ApplicationOwnershipQuery, Application> {
}
