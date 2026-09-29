package co.edu.uco.seguridad.pdp.profiles.domain.rule;

import co.edu.uco.seguridad.pdp.profiles.domain.rule.model.ProfileNameAvailability;
import co.edu.uco.seguridad.shared.contract.OperationWithoutResult;

/**
 * El nombre de perfil es único dentro de su alcance exacto (nivel + inquilino + aplicación).
 */
public interface ProfileNameMustBeUniqueInScopeRule extends OperationWithoutResult<ProfileNameAvailability> {
}
