package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.request.raw.RegisterApplicationWithInitialResourceRawRequest;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.response.ApplicationWithInitialResourceWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface RegisterApplicationWithInitialResourceInteractor extends
        ReactiveOperation<RegisterApplicationWithInitialResourceRawRequest, ApplicationWithInitialResourceWebResponse> {
}
