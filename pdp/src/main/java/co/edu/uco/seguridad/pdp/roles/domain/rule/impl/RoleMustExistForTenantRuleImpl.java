package co.edu.uco.seguridad.pdp.roles.domain.rule.impl;

import co.edu.uco.seguridad.pdp.roles.domain.exception.RoleNotFoundException;
import co.edu.uco.seguridad.pdp.roles.domain.rule.RoleMustExistForTenantRule;
import co.edu.uco.seguridad.pdp.roles.domain.rule.model.RoleExistence;

public final class RoleMustExistForTenantRuleImpl implements RoleMustExistForTenantRule {

    @Override
    public void execute(RoleExistence input) {
        if (!input.registered()) {
            throw new RoleNotFoundException(input.roleId());
        }
    }
}
