package co.edu.uco.seguridad.pdp.identity.application.secondaryport.repository;

import co.edu.uco.seguridad.pdp.identity.domain.model.Email;
import co.edu.uco.seguridad.pdp.identity.domain.model.ExternalIdentity;
import co.edu.uco.seguridad.pdp.identity.domain.SecurityUser;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Puerto secundario para almacenamiento de usuarios propios e identidades externas vinculadas. */
public interface SecurityUserRepository {

    Mono<ExternalIdentity> findIdentity(String issuer, String subject);

    /**
     * Complemento de {@link #findIdentity(String, String)} para cuando no se conoce el emisor (HU-015,
     * enmienda §14: el principal de un JWT crudo no lo trae). Vacío si no hay ninguna identidad
     * externa con ese subject.
     */
    Mono<ExternalIdentity> findIdentityBySubject(String subject);

    Mono<SecurityUser> findByEmail(Email email);

    Mono<SecurityUser> findById(UserId userId);

    Mono<String> providerFor(UserId userId);

    Mono<SecurityUser> save(SecurityUser user);

    Mono<Void> linkIdentity(ExternalIdentity identity);

    Flux<SecurityUser> findAll();
}
