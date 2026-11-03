package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ApplicationAdministrationRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.AdministeredApplicationWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface ApplicationCredentialRotationInteractor
        extends ReactiveOperation<ApplicationAdministrationRawRequest, AdministeredApplicationWebResponse> {
}
