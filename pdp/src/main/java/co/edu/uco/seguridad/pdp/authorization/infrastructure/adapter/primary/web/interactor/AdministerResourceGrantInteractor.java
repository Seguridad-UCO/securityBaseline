package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.GrantResourceRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.RoleAdministrationWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface AdministerResourceGrantInteractor
        extends ReactiveOperation<GrantResourceRawRequest, RoleAdministrationWebResponse> {
    default reactor.core.publisher.Mono<RoleAdministrationWebResponse> revoke(GrantResourceRawRequest input) {
        return reactor.core.publisher.Mono.error(new IllegalStateException("La revocación de recursos no está configurada"));
    }
}
