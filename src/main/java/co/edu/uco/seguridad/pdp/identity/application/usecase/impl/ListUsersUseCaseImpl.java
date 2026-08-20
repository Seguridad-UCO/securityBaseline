package co.edu.uco.seguridad.pdp.identity.application.usecase.impl;

import co.edu.uco.seguridad.pdp.identity.application.port.primary.dto.response.UserResponse;
import co.edu.uco.seguridad.pdp.identity.application.port.secondary.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.identity.application.usecase.ListUsersUseCase;
import co.edu.uco.seguridad.pdp.identity.domain.SecurityUser;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

public final class ListUsersUseCaseImpl implements ListUsersUseCase {

    private final SecurityUserRepository repository;

    public ListUsersUseCaseImpl(SecurityUserRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public Mono<List<UserResponse>> execute() {
        return repository.findAll()
                .concatMap(user -> repository.providerFor(user.id()).map(provider -> toResponse(user, provider)))
                .collectList();
    }

    private static UserResponse toResponse(SecurityUser user, String provider) {
        return new UserResponse(user.id(), user.email().value(), user.name(), provider, user.tenantId(),
                user.createdAt(), user.lastLoginAt());
    }
}
