package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.RegisterProtectedResourceRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.AdministeredResourceWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface AdministerResourceRegistrationInteractor
        extends ReactiveOperation<RegisterProtectedResourceRawRequest, AdministeredResourceWebResponse> {
}
