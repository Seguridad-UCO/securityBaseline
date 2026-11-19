package co.edu.uco.seguridad.pdp.profiles.application.usecase;

import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.ListApplicationProfilesPageRequest;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface ListApplicationProfilesPageUseCase extends ReactiveOperation<ListApplicationProfilesPageRequest, ResultPage<ProfileResponse>> {
}
