package co.edu.uco.seguridad.pdp.applications.application.rule.validator;

import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Resuelve la aplicación configurada por nombre en el canal interno del PEP.
 */
public interface ApplicationNameLookupValidator extends ReactiveOperation<ApplicationName, Application> {
}
