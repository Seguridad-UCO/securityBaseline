package co.edu.uco.seguridad.pdp.identity.application.usecase.impl;

import co.edu.uco.seguridad.pdp.identity.application.primaryport.request.AssignTenantRequest;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.response.UserResponse;
import co.edu.uco.seguridad.pdp.identity.application.secondaryport.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.identity.application.usecase.AssignTenantUseCase;
import co.edu.uco.seguridad.pdp.identity.domain.SecurityUser;
import co.edu.uco.seguridad.pdp.identity.domain.rule.UserMustExistRule;
import co.edu.uco.seguridad.pdp.identity.domain.rule.model.UserExistence;
import co.edu.uco.seguridad.pdp.tenants.application.rule.validator.TenantMustBeActiveValidator;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Confirma que el tenant destino existe y está activo (contrato publicado por {@code tenants}) antes
 * de reasignar.
 *
 * <p>El usuario se trae una sola vez: hace falta el agregado para reasignarlo, así que la ausencia
 * se resuelve sobre esa misma consulta en lugar de preguntar primero si existe.</p>
 */
public final class AssignTenantUseCaseImpl implements AssignTenantUseCase {

    private final TenantMustBeActiveValidator tenantMustBeActive;
    private final UserMustExistRule userMustExist;
    private final SecurityUserRepository repository;

    public AssignTenantUseCaseImpl(TenantMustBeActiveValidator tenantMustBeActive, UserMustExistRule userMustExist,
                                   SecurityUserRepository repository) {
        this.tenantMustBeActive = Objects.requireNonNull(tenantMustBeActive, RequiredArgumentMessages.TENANT_ACTIVE_VALIDATOR);
        this.userMustExist = Objects.requireNonNull(userMustExist, RequiredArgumentMessages.USER_RULE);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.USER_REPOSITORY);
    }

    @Override
    public Mono<UserResponse> execute(AssignTenantRequest dto) {
        // La rama vacía nunca completa: UserMustExistRule con registered=false siempre lanza.
        return tenantMustBeActive.execute(dto.tenantId())
                .then(repository.findById(dto.userId()))
                .switchIfEmpty(Mono.fromRunnable(
                        () -> userMustExist.execute(new UserExistence(dto.userId(), false))))
                .map(user -> user.withTenant(dto.tenantId()))
                .flatMap(repository::save)
                .flatMap(saved -> repository.providerFor(saved.id()).map(provider -> toResponse(saved, provider)));
    }

    private static UserResponse toResponse(SecurityUser user, String provider) {
        return new UserResponse(user.id(), user.email().value(), user.name(), provider, user.tenantId(),
                user.createdAt(), user.lastLoginAt());
    }
}
