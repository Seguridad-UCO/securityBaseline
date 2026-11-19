package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AddRoleToProfileRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ProfileAdministrationWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface AdministerProfileRoleAdditionInteractor
        extends ReactiveOperation<AddRoleToProfileRawRequest, ProfileAdministrationWebResponse> {
    default reactor.core.publisher.Mono<ProfileAdministrationWebResponse> remove(AddRoleToProfileRawRequest input) {
        return reactor.core.publisher.Mono.error(new IllegalStateException("La revocación de roles del perfil no está configurada"));
    }
}
