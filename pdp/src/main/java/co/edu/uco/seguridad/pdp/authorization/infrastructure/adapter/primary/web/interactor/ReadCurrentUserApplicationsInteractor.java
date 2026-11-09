package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.CurrentUserApplicationsWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutInput;

public interface ReadCurrentUserApplicationsInteractor extends ReactiveOperationWithoutInput<CurrentUserApplicationsWebResponse> {
}
