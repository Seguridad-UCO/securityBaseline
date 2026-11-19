package co.edu.uco.seguridad.pdp.resources.application.usecase;

import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.RegisterApplicationWithInitialResourceRequest;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.response.ApplicationWithInitialResourceRegistrationResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface RegisterApplicationWithInitialResourceUseCase extends
        ReactiveOperation<RegisterApplicationWithInitialResourceRequest, ApplicationWithInitialResourceRegistrationResponse> {
}
