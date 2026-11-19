package co.edu.uco.seguridad.pdp.profiles.application.rule.validator;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.ProfileOwnershipQuery;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

import java.util.Optional;

/**
 * Resuelve a qué aplicación pertenece un perfil, si a alguna (HU-019). Espejo exacto de
 * {@code RoleApplicationLookupValidator} (HU-016), aplicado a {@code Profile.scope()} — vacío para
 * alcance {@code TENANT}.
 */
public interface ProfileApplicationLookupValidator extends ReactiveOperation<ProfileOwnershipQuery, Optional<ApplicationId>> {
}
