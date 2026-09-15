package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AssignRoleRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.AssignmentAdministrationWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface AdministerAssignmentCreationInteractor
        extends ReactiveOperation<AssignRoleRawRequest, AssignmentAdministrationWebResponse> {
}
