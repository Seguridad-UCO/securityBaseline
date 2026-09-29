package co.edu.uco.seguridad.pdp.authorization.application.service;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AccessRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.PolicyEvaluationInput;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Resuelve los hechos de autorización antes de salir del PDP.
 */
public interface AuthorizationContextResolver extends ReactiveOperation<AccessRequest, PolicyEvaluationInput> {
}
