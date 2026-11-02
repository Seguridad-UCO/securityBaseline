package co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.usecase;

import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.request.CreateServiceIdentityRequest;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.response.ServiceIdentityResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface CreateServiceIdentityUseCase extends ReactiveOperation<CreateServiceIdentityRequest, ServiceIdentityResponse> {
}
