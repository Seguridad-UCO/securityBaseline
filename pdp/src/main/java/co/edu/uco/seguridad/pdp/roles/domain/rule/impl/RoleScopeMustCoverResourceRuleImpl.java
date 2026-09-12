package co.edu.uco.seguridad.pdp.roles.domain.rule.impl;

import co.edu.uco.seguridad.pdp.roles.domain.exception.ResourceOutsideRoleScopeException;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScopeLevel;
import co.edu.uco.seguridad.pdp.roles.domain.rule.RoleScopeMustCoverResourceRule;
import co.edu.uco.seguridad.pdp.roles.domain.rule.model.ResourceCoverage;

/** R5 (INV-DAT-01): global cubre todo; tenant cubre sus aplicaciones; aplicación cubre solo la suya. */
public final class RoleScopeMustCoverResourceRuleImpl implements RoleScopeMustCoverResourceRule {

    @Override
    public void execute(ResourceCoverage input) {
        RoleScope scope = input.scope();
        if (scope.isGlobal()) {
            return;
        }
        boolean covered = scope.level() == RoleScopeLevel.APPLICATION
                ? scope.applicationId().map(applicationId -> applicationId.equals(input.resourceApplicationId())).orElse(false)
                : scope.tenantId().map(tenantId -> tenantId.equals(input.resourceTenantId())).orElse(false);
        if (!covered) {
            throw new ResourceOutsideRoleScopeException(input.resourceApplicationId());
        }
    }
}
