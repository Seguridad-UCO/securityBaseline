package co.edu.uco.seguridad.pdp.authorization.application.usecase;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.AdministrationDecision;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface AuthorizeAdministrationUseCase
        extends ReactiveOperation<AdministrationRequest, AdministrationDecision> {
}
