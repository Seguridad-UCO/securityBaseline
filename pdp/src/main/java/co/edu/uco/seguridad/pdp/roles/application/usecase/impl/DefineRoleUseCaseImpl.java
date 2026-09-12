package co.edu.uco.seguridad.pdp.roles.application.usecase.impl;

import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.DefineRoleRequest;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.DefineRoleRulesValidator;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.application.usecase.DefineRoleUseCase;
import co.edu.uco.seguridad.pdp.roles.domain.Role;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Construye un Role desde cero: por eso IdentifierGenerator y TimeProvider van en la firma desde el primer día. */
public final class DefineRoleUseCaseImpl implements DefineRoleUseCase {

    private final DefineRoleRulesValidator rules;
    private final RoleRepository repository;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public DefineRoleUseCaseImpl(DefineRoleRulesValidator rules, RoleRepository repository,
            IdentifierGenerator identifiers, TimeProvider time) {
        this.rules = Objects.requireNonNull(rules, RequiredArgumentMessages.DEFINE_ROLE_RULES_VALIDATOR);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.ROLE_REPOSITORY);
        this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<RoleResponse> execute(DefineRoleRequest input) {
        return rules.execute(input)
                .then(Mono.defer(() -> repository.save(Role.define(
                        new RoleId(identifiers.next()), input.name(), input.scope(), time.now()))))
                .map(role -> new RoleResponse(role.id(), role.name(), role.scope(), role.resources(), role.registeredAt()));
    }
}
