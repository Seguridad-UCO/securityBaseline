package co.edu.uco.seguridad.pdp.roles.application.usecase.impl;

import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.GrantResourceRequest;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.GrantResourceRulesValidator;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.application.usecase.GrantResourceToRoleUseCase;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Transforma lo que el validador ya encontró y validó (Role.withResource) y lo guarda. No decide nada. */
public final class GrantResourceToRoleUseCaseImpl implements GrantResourceToRoleUseCase {

    private final GrantResourceRulesValidator rules;
    private final RoleRepository repository;

    public GrantResourceToRoleUseCaseImpl(GrantResourceRulesValidator rules, RoleRepository repository) {
        this.rules = Objects.requireNonNull(rules, RequiredArgumentMessages.GRANT_RESOURCE_RULES_VALIDATOR);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.ROLE_REPOSITORY);
    }

    @Override
    public Mono<RoleResponse> execute(GrantResourceRequest input) {
        return rules.execute(input)
                .map(role -> role.withResource(input.resourceId()))
                .flatMap(repository::save)
                .map(role -> new RoleResponse(role.id(), role.name(), role.scope(), role.resources(), role.registeredAt()));
    }
}
