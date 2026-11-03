package co.edu.uco.seguridad.pdp.roles.domain.rule.impl;

import co.edu.uco.seguridad.pdp.roles.domain.exception.DuplicateRoleNameException;
import co.edu.uco.seguridad.pdp.roles.domain.rule.RoleNameMustBeUniqueInScopeRule;
import co.edu.uco.seguridad.pdp.roles.domain.rule.model.RoleNameAvailability;

public final class RoleNameMustBeUniqueInScopeRuleImpl implements RoleNameMustBeUniqueInScopeRule {

    @Override
    public void execute(RoleNameAvailability input) {
        if (input.taken()) {
            throw new DuplicateRoleNameException(input.name(), input.scope());
        }
    }
}
