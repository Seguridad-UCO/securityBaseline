package co.edu.uco.seguridad.pdp.roles.application.rule.validator;

import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.GrantResourceRequest;
import co.edu.uco.seguridad.pdp.roles.domain.Role;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Finder con rechazo (mismo patrón que ApplicationOwnerLookupValidator): valida R3, R4 y R5 y
 * devuelve el rol encontrado para el inquilino, que el caso de uso transforma y guarda.
 */
public interface GrantResourceRulesValidator extends ReactiveOperation<GrantResourceRequest, Role> {
}
