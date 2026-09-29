package co.edu.uco.seguridad.pdp.applications.application.rule.validator;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Publica la mínima proyección de una aplicación para consumidores autorizados entre módulos.
 */
public interface ApplicationDetailsLookupValidator extends ReactiveOperation<ApplicationId, RegisteredApplicationResponse> {
}
