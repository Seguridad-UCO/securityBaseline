package co.edu.uco.seguridad.pdp.roles.application.rule.validator;

import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

public interface ResourceDeletionDependencyValidator extends ReactiveOperationWithoutResult<ResourceId> {
}
