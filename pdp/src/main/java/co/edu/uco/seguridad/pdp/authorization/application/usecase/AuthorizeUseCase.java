package co.edu.uco.seguridad.pdp.authorization.application.usecase;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AccessRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.AccessDecision;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface AuthorizeUseCase extends ReactiveOperation<AccessRequest, AccessDecision> {
}
