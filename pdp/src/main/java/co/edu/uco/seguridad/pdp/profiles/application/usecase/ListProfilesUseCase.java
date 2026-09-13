package co.edu.uco.seguridad.pdp.profiles.application.usecase;

import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.ListProfilesRequest;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface ListProfilesUseCase extends ReactiveOperation<ListProfilesRequest, ResultPage<ProfileResponse>> {
}
