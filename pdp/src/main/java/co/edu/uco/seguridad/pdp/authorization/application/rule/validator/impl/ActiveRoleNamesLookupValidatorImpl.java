package co.edu.uco.seguridad.pdp.authorization.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ResolveActiveRolesRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ActiveRolesResponse;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ResolveActiveRolesUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.ActiveRoleNamesLookupValidator;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleNamesLookupValidator;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Set;

public final class ActiveRoleNamesLookupValidatorImpl implements ActiveRoleNamesLookupValidator {

    private final ResolveActiveRolesUseCase resolveActiveRoles;
    private final RoleNamesLookupValidator roleNamesLookup;

    public ActiveRoleNamesLookupValidatorImpl(ResolveActiveRolesUseCase resolveActiveRoles,
                                              RoleNamesLookupValidator roleNamesLookup) {
        this.resolveActiveRoles = Objects.requireNonNull(resolveActiveRoles, RequiredArgumentMessages.RESOLVE_ACTIVE_ROLES_USE_CASE);
        this.roleNamesLookup = Objects.requireNonNull(roleNamesLookup, RequiredArgumentMessages.ROLE_NAMES_LOOKUP_VALIDATOR);
    }

    @Override
    public Mono<Set<String>> execute(ResolveActiveRolesRequest input) {
        return resolveActiveRoles.execute(input)
                .map(ActiveRolesResponse::roleIds)
                .flatMap(roleNamesLookup::execute);
    }
}
