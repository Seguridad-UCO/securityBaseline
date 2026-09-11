package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AuthorizeRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.AccessDecisionWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface AuthorizeInteractor extends ReactiveOperation<AuthorizeRawRequest, AccessDecisionWebResponse> {
}
