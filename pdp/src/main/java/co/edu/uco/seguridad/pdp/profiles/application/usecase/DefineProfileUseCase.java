package co.edu.uco.seguridad.pdp.profiles.application.usecase;

import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.DefineProfileRequest;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface DefineProfileUseCase extends ReactiveOperation<DefineProfileRequest, ProfileResponse> {
}
