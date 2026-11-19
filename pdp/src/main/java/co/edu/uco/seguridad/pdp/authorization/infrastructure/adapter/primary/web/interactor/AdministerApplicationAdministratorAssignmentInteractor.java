package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AssignApplicationAdministratorRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ApplicationAdministratorWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface AdministerApplicationAdministratorAssignmentInteractor
        extends ReactiveOperation<AssignApplicationAdministratorRawRequest, ApplicationAdministratorWebResponse> {
}
