package co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.request.raw.ListRolesRawRequest;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.response.RoleWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;
import co.edu.uco.seguridad.shared.web.PageResponse;

public interface ListRolesInteractor extends ReactiveOperation<ListRolesRawRequest, PageResponse<RoleWebResponse>> {
}
