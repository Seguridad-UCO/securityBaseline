package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.AssignProfileRawRequest;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.response.ProfileAssignmentWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface AssignProfileInteractor extends ReactiveOperation<AssignProfileRawRequest, ProfileAssignmentWebResponse> {
}
