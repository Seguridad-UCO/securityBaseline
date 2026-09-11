package co.edu.uco.seguridad.pdp.resources.application.rule.validator;

import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.ProtectedResourceLookup;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

/**
 * Contrato publicado a los demas modulos: el recurso protegido existe bajo esa aplicacion. Lo
 * consume {@code authorization}, igual que {@code applications} publica
 * {@code ApplicationMustExistForTenantValidator}.
 */
public interface ProtectedResourceMustExistValidator extends ReactiveOperationWithoutResult<ProtectedResourceLookup> {
}
