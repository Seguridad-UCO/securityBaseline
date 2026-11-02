package co.edu.uco.seguridad.pdp.profiles.application.usecase;

import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.FindProfileForTenantRequest;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface FindProfileForTenantUseCase extends ReactiveOperation<FindProfileForTenantRequest, ProfileResponse> {
}
