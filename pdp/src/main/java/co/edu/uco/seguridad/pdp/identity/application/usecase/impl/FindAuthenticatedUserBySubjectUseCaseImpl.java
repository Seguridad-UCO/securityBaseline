package co.edu.uco.seguridad.pdp.identity.application.usecase.impl;

import co.edu.uco.seguridad.pdp.identity.application.primaryport.response.AuthenticatedUserResponse;
import co.edu.uco.seguridad.pdp.identity.application.secondaryport.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.identity.application.usecase.FindAuthenticatedUserBySubjectUseCase;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class FindAuthenticatedUserBySubjectUseCaseImpl implements FindAuthenticatedUserBySubjectUseCase {
    private final SecurityUserRepository users;

    public FindAuthenticatedUserBySubjectUseCaseImpl(SecurityUserRepository users) {
        this.users = Objects.requireNonNull(users);
    }

    @Override
    public Mono<AuthenticatedUserResponse> execute(String subject) {
        return users.findUserBySubject(subject)
                .map(user -> new AuthenticatedUserResponse(user.id(), user.tenantId(), user.name(), user.email().value()));
    }
}
