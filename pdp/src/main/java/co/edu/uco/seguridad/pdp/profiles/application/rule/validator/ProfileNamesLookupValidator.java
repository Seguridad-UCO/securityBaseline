package co.edu.uco.seguridad.pdp.profiles.application.rule.validator;

import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

import java.util.Set;

/**
 * Proyección publicada de perfiles efectivos a nombres para OPA.
 */
public interface ProfileNamesLookupValidator extends ReactiveOperation<Set<ProfileId>, Set<String>> {
}
