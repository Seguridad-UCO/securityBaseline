package co.edu.uco.seguridad.pdp.resources.application.rule.validator;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

/** Impide borrar una aplicación mientras conserve recursos protegidos. */
public interface ApplicationDeletionDependencyValidator extends ReactiveOperationWithoutResult<ApplicationId> {
}
