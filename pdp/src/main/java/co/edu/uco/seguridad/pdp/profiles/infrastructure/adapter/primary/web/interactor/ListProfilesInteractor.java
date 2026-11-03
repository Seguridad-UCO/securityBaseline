package co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.request.raw.ListProfilesRawRequest;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.response.ProfileWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;
import co.edu.uco.seguridad.shared.web.PageResponse;

public interface ListProfilesInteractor extends ReactiveOperation<ListProfilesRawRequest, PageResponse<ProfileWebResponse>> {
}
