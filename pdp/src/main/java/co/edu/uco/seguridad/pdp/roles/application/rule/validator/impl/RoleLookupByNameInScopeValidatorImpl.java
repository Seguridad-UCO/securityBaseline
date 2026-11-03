package co.edu.uco.seguridad.pdp.roles.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.RoleNameInScopeQuery;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleLookupByNameInScopeValidator;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.domain.Role;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class RoleLookupByNameInScopeValidatorImpl implements RoleLookupByNameInScopeValidator {

    private final RoleRepository repository;

    public RoleLookupByNameInScopeValidatorImpl(RoleRepository repository) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.ROLE_REPOSITORY);
    }

    @Override
    public Mono<RoleId> execute(RoleNameInScopeQuery query) {
        return repository.findByNameInScope(query.name(), query.scope()).map(Role::id);
    }
}
