package co.edu.uco.seguridad.pdp.identity.application.port.secondary.repository;

import co.edu.uco.seguridad.pdp.identity.domain.Email;
import co.edu.uco.seguridad.pdp.identity.domain.ExternalIdentity;
import co.edu.uco.seguridad.pdp.identity.domain.SecurityUser;
import co.edu.uco.seguridad.pdp.identity.domain.UserId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Puerto secundario para almacenamiento de usuarios propios e identidades externas vinculadas. */
public interface SecurityUserRepository {

    Mono<ExternalIdentity> findIdentity(String issuer, String subject);

    Mono<SecurityUser> findByEmail(Email email);

    Mono<SecurityUser> findById(UserId userId);

    Mono<String> providerFor(UserId userId);

    Mono<SecurityUser> save(SecurityUser user);

    Mono<Void> linkIdentity(ExternalIdentity identity);

    Flux<SecurityUser> findAll();
}
