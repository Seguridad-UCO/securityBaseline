package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ListApplicationSecurityRelationRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.*;
import co.edu.uco.seguridad.shared.web.PageResponse;
import reactor.core.publisher.Mono;
public interface AssignmentDetailReadInteractor {
 Mono<PageResponse<ApplicationSecurityRoleAssignmentWebResponse>> roles(ListApplicationSecurityRelationRawRequest request);
 Mono<PageResponse<ApplicationSecurityProfileAssignmentWebResponse>> profiles(ListApplicationSecurityRelationRawRequest request);
 Mono<ApplicationSecurityUserAssignmentsWebResponse> user(ListApplicationSecurityRelationRawRequest request);
}
