package co.edu.uco.seguridad.pdp.profiles.domain.rule;

import co.edu.uco.seguridad.pdp.profiles.domain.rule.model.ProfileExistence;
import co.edu.uco.seguridad.shared.contract.OperationWithoutResult;

/** El perfil existe para ese inquilino. */
public interface ProfileMustExistForTenantRule extends OperationWithoutResult<ProfileExistence> {
}
