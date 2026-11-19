package co.edu.uco.seguridad.pdp.authorization.application.usecase;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerProfileRoleAdditionRequest;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface AdministerProfileRoleAdditionUseCase
        extends ReactiveOperation<AdministerProfileRoleAdditionRequest, ProfileResponse> {
}
