package co.edu.uco.seguridad.pdp.profiles.application.rule.validator;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

/** Impide borrar una aplicación mientras tenga perfiles propios. */
public interface ApplicationDeletionDependencyValidator extends ReactiveOperationWithoutResult<ApplicationId> {
}
