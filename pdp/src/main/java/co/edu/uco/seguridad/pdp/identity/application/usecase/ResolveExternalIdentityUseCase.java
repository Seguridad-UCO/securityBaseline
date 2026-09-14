package co.edu.uco.seguridad.pdp.identity.application.usecase;

import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.request.ResolveExternalIdentityRequest;
import reactor.core.publisher.Mono;

import java.util.Optional;

/** Resuelve el identificador local asociado a un {@code issuer + sub} de un IdP. */
public interface ResolveExternalIdentityUseCase {

    Mono<Optional<UserId>> execute(ResolveExternalIdentityRequest input);
}
