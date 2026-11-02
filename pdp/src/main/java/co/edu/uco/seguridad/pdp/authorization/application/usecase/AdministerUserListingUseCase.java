package co.edu.uco.seguridad.pdp.authorization.application.usecase;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.TenantAdministrationRequest;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.response.UserResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

import java.util.List;

public interface AdministerUserListingUseCase
        extends ReactiveOperation<TenantAdministrationRequest, List<UserResponse>> {
}
