package co.edu.uco.seguridad.pdp.roles.application.rule.validator;

import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.RoleOwnershipQuery;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

/**
 * Contrato publicado a otros módulos (HU-011: {@code profiles}): el rol existe para ese inquilino.
 * Envuelve la regla pura {@code RoleMustExistForTenantRule}, que ya existía pero solo se usaba
 * dentro de {@code roles} (en {@code GrantResourceRulesValidatorImpl}) — nunca se había publicado.
 */
public interface RoleMustExistForTenantValidator extends ReactiveOperationWithoutResult<RoleOwnershipQuery> {
}
