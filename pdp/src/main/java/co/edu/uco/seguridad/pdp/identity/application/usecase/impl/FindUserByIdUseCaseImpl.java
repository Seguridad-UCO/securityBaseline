package co.edu.uco.seguridad.pdp.identity.application.usecase.impl;

import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.response.UserResponse;
import co.edu.uco.seguridad.pdp.identity.application.secondaryport.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.identity.application.usecase.FindUserByIdUseCase;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class FindUserByIdUseCaseImpl implements FindUserByIdUseCase {
    private final SecurityUserRepository users;

    public FindUserByIdUseCaseImpl(SecurityUserRepository users) {
        this.users = Objects.requireNonNull(users);
    }

    @Override
    public Mono<UserResponse> execute(UserId userId) {
        return users.findById(userId).map(user -> new UserResponse(user.id(), user.email().value(), user.name(), "",
                user.tenantId(), user.createdAt(), user.lastLoginAt()));
    }
}
