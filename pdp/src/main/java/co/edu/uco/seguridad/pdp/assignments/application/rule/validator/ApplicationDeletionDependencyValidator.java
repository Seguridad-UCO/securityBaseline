package co.edu.uco.seguridad.pdp.assignments.application.rule.validator;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

/** Impide borrar una aplicación mientras conserve asignaciones de rol o de perfil. */
public interface ApplicationDeletionDependencyValidator extends ReactiveOperationWithoutResult<ApplicationId> {
}
