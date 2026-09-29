package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ListApplicationSecurityRelationRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ApplicationSecurityResourceWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation; import co.edu.uco.seguridad.shared.web.PageResponse;
public interface ListRoleResourcesInteractor extends ReactiveOperation<ListApplicationSecurityRelationRawRequest, PageResponse<ApplicationSecurityResourceWebResponse>> { }
