package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AccessDecisionRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.AccessDecisionInternalWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface InternalAccessDecisionInteractor
        extends ReactiveOperation<AccessDecisionRawRequest, AccessDecisionInternalWebResponse> {
}
