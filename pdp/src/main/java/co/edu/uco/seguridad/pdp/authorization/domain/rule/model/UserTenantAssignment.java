package co.edu.uco.seguridad.pdp.authorization.domain.rule.model;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Los tres inquilinos que decide la reasignación de un usuario (SU-01): el del actor, el que el
 * usuario tiene hoy y el que se le quiere asignar.
 */
public record UserTenantAssignment(TenantId actorTenant, TenantId currentTenant, TenantId targetTenant) {

    public UserTenantAssignment {
        Objects.requireNonNull(actorTenant, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(currentTenant, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(targetTenant, RequiredArgumentMessages.TENANT_ID);
    }
}
