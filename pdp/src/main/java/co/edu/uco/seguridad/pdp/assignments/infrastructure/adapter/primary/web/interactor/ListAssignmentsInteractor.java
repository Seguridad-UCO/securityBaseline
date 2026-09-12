package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.ListAssignmentsRawRequest;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.response.AssignmentWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;
import co.edu.uco.seguridad.shared.web.PageResponse;

public interface ListAssignmentsInteractor extends ReactiveOperation<ListAssignmentsRawRequest, PageResponse<AssignmentWebResponse>> {
}
