package co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.request.raw.DefineProfileRawRequest;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.response.ProfileWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface DefineProfileInteractor extends ReactiveOperation<DefineProfileRawRequest, ProfileWebResponse> {
}
