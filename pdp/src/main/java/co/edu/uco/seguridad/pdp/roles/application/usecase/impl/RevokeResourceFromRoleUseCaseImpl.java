package co.edu.uco.seguridad.pdp.roles.application.usecase.impl;

import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.RevokeResourceRequest;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.GrantResourceRulesValidator;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.application.usecase.RevokeResourceFromRoleUseCase;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Reutiliza las mismas comprobaciones de pertenencia y alcance que la concesión. */
public final class RevokeResourceFromRoleUseCaseImpl implements RevokeResourceFromRoleUseCase {
    private final GrantResourceRulesValidator rules;
    private final RoleRepository repository;
    public RevokeResourceFromRoleUseCaseImpl(GrantResourceRulesValidator rules, RoleRepository repository) {
        this.rules = Objects.requireNonNull(rules, RequiredArgumentMessages.GRANT_RESOURCE_RULES_VALIDATOR);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.ROLE_REPOSITORY);
    }
    @Override public Mono<RoleResponse> execute(RevokeResourceRequest input) {
        return rules.execute(new co.edu.uco.seguridad.pdp.roles.application.primaryport.request.GrantResourceRequest(input.tenantId(), input.roleId(), input.resourceId()))
                .map(role -> role.withoutResource(input.resourceId())).flatMap(repository::save)
                .map(role -> new RoleResponse(role.id(), role.name(), role.scope(), role.resources(), role.registeredAt()));
    }
}
