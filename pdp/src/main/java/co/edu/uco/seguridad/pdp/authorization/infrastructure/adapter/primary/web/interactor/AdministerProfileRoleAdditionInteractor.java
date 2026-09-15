package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AddRoleToProfileRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ProfileAdministrationWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface AdministerProfileRoleAdditionInteractor
        extends ReactiveOperation<AddRoleToProfileRawRequest, ProfileAdministrationWebResponse> {
}
