package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AssignTenantRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.UserWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Adaptador primario HTTP: reasigna el inquilino de un usuario en nombre del principal (SU-01).
 */
public interface AdministerUserTenantAssignmentInteractor extends ReactiveOperation<AssignTenantRawRequest, UserWebResponse> {
}
