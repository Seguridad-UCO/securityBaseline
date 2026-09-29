package co.edu.uco.seguridad.pdp.roles.application.rule.validator;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

/**
 * Impide borrar una aplicación mientras tenga roles propios.
 */
public interface ApplicationDeletionDependencyValidator extends ReactiveOperationWithoutResult<ApplicationId> {
}
