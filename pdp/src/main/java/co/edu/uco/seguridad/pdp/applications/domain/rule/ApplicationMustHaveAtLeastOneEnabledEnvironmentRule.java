package co.edu.uco.seguridad.pdp.applications.domain.rule;

import co.edu.uco.seguridad.pdp.applications.domain.rule.model.ApplicationEnvironmentSelection;
import co.edu.uco.seguridad.shared.contract.OperationWithoutResult;

/** Una aplicación protegida debe estar disponible en por lo menos un ambiente. */
public interface ApplicationMustHaveAtLeastOneEnabledEnvironmentRule
        extends OperationWithoutResult<ApplicationEnvironmentSelection> {
}
