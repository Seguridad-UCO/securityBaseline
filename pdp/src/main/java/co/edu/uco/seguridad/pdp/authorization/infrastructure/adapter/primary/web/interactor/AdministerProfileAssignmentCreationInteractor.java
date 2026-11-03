package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AssignProfileRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ProfileAssignmentAdministrationWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface AdministerProfileAssignmentCreationInteractor
        extends ReactiveOperation<AssignProfileRawRequest, ProfileAssignmentAdministrationWebResponse> {
}
