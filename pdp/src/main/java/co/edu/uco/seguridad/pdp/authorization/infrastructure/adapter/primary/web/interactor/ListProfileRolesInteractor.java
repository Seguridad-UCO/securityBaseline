package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ListApplicationSecurityRelationRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ApplicationSecurityRoleWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation; import co.edu.uco.seguridad.shared.web.PageResponse;
public interface ListProfileRolesInteractor extends ReactiveOperation<ListApplicationSecurityRelationRawRequest, PageResponse<ApplicationSecurityRoleWebResponse>> { }
