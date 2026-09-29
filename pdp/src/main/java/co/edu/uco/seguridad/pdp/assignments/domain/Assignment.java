package co.edu.uco.seguridad.pdp.assignments.domain;

import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.assignments.domain.model.Validity;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.time.Instant;
import java.util.Objects;

/**
 * Entrada del catálogo de asignaciones (BC-08): liga un usuario, una aplicación y un rol, con una
 * vigencia. El tenant se fija una sola vez, en el momento de crear, a partir del tenant ya validado
 * de la aplicación — nunca es un input independiente (ver PLAN-HU-005 §4 y §11.1).
 */
public record Assignment(AssignmentId id, UserId userId, TenantId tenantId, ApplicationId applicationId,
                         RoleId roleId, Validity validity) {

    public Assignment {
        Objects.requireNonNull(id, RequiredArgumentMessages.ASSIGNMENT_ID);
        Objects.requireNonNull(userId, RequiredArgumentMessages.USER_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
        Objects.requireNonNull(roleId, RequiredArgumentMessages.ROLE_ID);
        Objects.requireNonNull(validity, RequiredArgumentMessages.VALIDITY);
    }

    public static Assignment assign(AssignmentId id, UserId userId, TenantId tenantId, ApplicationId applicationId,
                                    RoleId roleId, Instant now) {
        return new Assignment(id, userId, tenantId, applicationId, roleId, Validity.startingNow(now));
    }

    public Assignment revoke(Instant now) {
        return new Assignment(id, userId, tenantId, applicationId, roleId, validity.endingAt(now));
    }

    public boolean isActive(Instant now) {
        return validity.isActiveAt(now);
    }
}
