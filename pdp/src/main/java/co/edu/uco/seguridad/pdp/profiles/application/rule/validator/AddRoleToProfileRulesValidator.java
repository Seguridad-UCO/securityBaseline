package co.edu.uco.seguridad.pdp.profiles.application.rule.validator;

import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.AddRoleToProfileRequest;
import co.edu.uco.seguridad.pdp.profiles.domain.Profile;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Devuelve el perfil encontrado para que el caso de uso lo transforme (mismo patrón que GrantResourceRulesValidator).
 */
public interface AddRoleToProfileRulesValidator extends ReactiveOperation<AddRoleToProfileRequest, Profile> {
}
