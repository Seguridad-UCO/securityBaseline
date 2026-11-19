package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.RegisterApplicationWithFirstAdministratorRawRequest;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.response.ApplicationRegisteredWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface RegisterApplicationWithFirstAdministratorInteractor
        extends ReactiveOperation<RegisterApplicationWithFirstAdministratorRawRequest, ApplicationRegisteredWebResponse> {
}
