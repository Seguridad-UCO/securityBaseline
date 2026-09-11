package co.edu.uco.seguridad.pdp.resources.application.rule.validator;

import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.RegisterProtectedResourceRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

/** Punto de entrada único a cada regla que protege el registro de un endpoint. */
public interface RegisterProtectedResourceRulesValidator extends ReactiveOperationWithoutResult<RegisterProtectedResourceRequest> {
}
