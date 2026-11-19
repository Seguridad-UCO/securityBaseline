package co.edu.uco.seguridad.pdp.authorization.application.usecase;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerUserTenantAssignmentRequest;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.response.UserResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface AdministerUserTenantAssignmentUseCase
        extends ReactiveOperation<AdministerUserTenantAssignmentRequest, UserResponse> {
}
