package co.edu.uco.seguridad.pdp.identity.application.usecase.impl;

import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.request.ResolveExternalIdentityRequest;
import co.edu.uco.seguridad.pdp.identity.application.secondaryport.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.identity.application.usecase.ResolveExternalIdentityUseCase;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;

/** Adaptación del vínculo persistido de identidad externa al identificador usado por autorización. */
public final class ResolveExternalIdentityUseCaseImpl implements ResolveExternalIdentityUseCase {

    private final SecurityUserRepository repository;

    public ResolveExternalIdentityUseCaseImpl(SecurityUserRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public Mono<Optional<UserId>> execute(ResolveExternalIdentityRequest input) {
        return repository.findIdentity(input.issuer(), input.subject())
                .map(identity -> Optional.of(identity.userId()))
                .defaultIfEmpty(Optional.empty());
    }
}
