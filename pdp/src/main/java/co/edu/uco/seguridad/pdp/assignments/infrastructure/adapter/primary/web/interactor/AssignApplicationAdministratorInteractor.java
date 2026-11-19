package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.AssignApplicationAdministratorRawRequest;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.response.AssignmentWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface AssignApplicationAdministratorInteractor
        extends ReactiveOperation<AssignApplicationAdministratorRawRequest, AssignmentWebResponse> {
}
