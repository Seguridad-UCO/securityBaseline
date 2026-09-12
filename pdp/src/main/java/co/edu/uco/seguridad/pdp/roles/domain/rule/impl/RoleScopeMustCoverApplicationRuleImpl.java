package co.edu.uco.seguridad.pdp.roles.domain.rule.impl;

import co.edu.uco.seguridad.pdp.roles.domain.exception.ApplicationOutsideRoleScopeException;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScopeLevel;
import co.edu.uco.seguridad.pdp.roles.domain.rule.RoleScopeMustCoverApplicationRule;
import co.edu.uco.seguridad.pdp.roles.domain.rule.model.ApplicationCoverage;

public final class RoleScopeMustCoverApplicationRuleImpl implements RoleScopeMustCoverApplicationRule {

    @Override
    public void execute(ApplicationCoverage input) {
        RoleScope scope = input.scope();
        if (scope.isGlobal()) {
            return;
        }
        boolean covered = scope.level() == RoleScopeLevel.APPLICATION
                ? scope.applicationId().map(applicationId -> applicationId.equals(input.applicationId())).orElse(false)
                : scope.tenantId().map(tenantId -> tenantId.equals(input.tenantId())).orElse(false);
        if (!covered) {
            throw new ApplicationOutsideRoleScopeException(input.applicationId());
        }
    }
}
