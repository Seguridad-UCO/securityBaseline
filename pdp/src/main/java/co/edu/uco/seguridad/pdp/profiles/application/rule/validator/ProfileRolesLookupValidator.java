package co.edu.uco.seguridad.pdp.profiles.application.rule.validator;

import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.ProfileOwnershipQuery;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

import java.util.Set;

/**
 * Contrato publicado a otros módulos (HU-011: {@code assignments}): el perfil existe para ese
 * inquilino, y devuelve el conjunto de roles que agrupa. A diferencia de
 * {@code RoleNamesLookupValidator}, un perfil inexistente **sí** rechaza (lanza
 * {@code ProfileNotFoundException}): aquí no es un enriquecimiento opcional, es la base de una
 * materialización que no puede ocurrir a medias sobre un perfil que no existe.
 */
public interface ProfileRolesLookupValidator extends ReactiveOperation<ProfileOwnershipQuery, Set<RoleId>> {
}
