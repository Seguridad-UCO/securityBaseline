package co.edu.uco.seguridad.pdp.authorization.domain.rule.impl;

import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerTenantCatalogException;
import co.edu.uco.seguridad.pdp.authorization.domain.rule.UserTenantAssignmentMustStayInActorTenantRule;
import co.edu.uco.seguridad.pdp.authorization.domain.rule.model.UserTenantAssignment;

import java.util.Optional;

public final class UserTenantAssignmentMustStayInActorTenantRuleImpl implements UserTenantAssignmentMustStayInActorTenantRule {

    @Override
    public void execute(UserTenantAssignment input) {
        if (!input.currentTenant().equals(input.actorTenant())) {
            throw new NotAuthorizedToAdministerTenantCatalogException(input.currentTenant(), Optional.empty());
        }
        if (!input.targetTenant().equals(input.actorTenant())) {
            throw new NotAuthorizedToAdministerTenantCatalogException(input.targetTenant(), Optional.empty());
        }
    }
}
