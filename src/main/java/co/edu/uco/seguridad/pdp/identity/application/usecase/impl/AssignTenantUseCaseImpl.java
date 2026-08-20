package co.edu.uco.seguridad.pdp.identity.application.usecase.impl;

import co.edu.uco.seguridad.pdp.identity.application.exception.UserNotFoundException;
import co.edu.uco.seguridad.pdp.identity.application.port.primary.dto.request.AssignTenantRequest;
import co.edu.uco.seguridad.pdp.identity.application.port.primary.dto.response.UserResponse;
import co.edu.uco.seguridad.pdp.identity.application.port.secondary.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.identity.application.usecase.AssignTenantUseCase;
import co.edu.uco.seguridad.pdp.identity.domain.SecurityUser;
import co.edu.uco.seguridad.pdp.tenants.application.rule.TenantMustBeActiveRule;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Confirma que el tenant destino existe y está activo (regla publicada por {@code tenants}) antes de reasignar. */
public final class AssignTenantUseCaseImpl implements AssignTenantUseCase {

    private final TenantMustBeActiveRule tenantMustBeActive;
    private final SecurityUserRepository repository;

    public AssignTenantUseCaseImpl(TenantMustBeActiveRule tenantMustBeActive, SecurityUserRepository repository) {
        this.tenantMustBeActive = Objects.requireNonNull(tenantMustBeActive);
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public Mono<UserResponse> execute(AssignTenantRequest dto) {
        return tenantMustBeActive.execute(dto.tenantId())
                .then(repository.findById(dto.userId()))
                .switchIfEmpty(Mono.error(() -> new UserNotFoundException(dto.userId())))
                .map(user -> user.withTenant(dto.tenantId()))
                .flatMap(repository::save)
                .flatMap(saved -> repository.providerFor(saved.id()).map(provider -> toResponse(saved, provider)));
    }

    private static UserResponse toResponse(SecurityUser user, String provider) {
        return new UserResponse(user.id(), user.email().value(), user.name(), provider, user.tenantId(),
                user.createdAt(), user.lastLoginAt());
    }
}
