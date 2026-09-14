package co.edu.uco.seguridad.pdp.assignments.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entrada de {@code AssignApplicationAdministratorUseCase} (HU-015): backfill manual del primer
 * administrador de una aplicación ya existente. {@code tenantId} lo resuelve el interactor con
 * {@code ApplicationOwnerLookupValidator}, nunca el cuerpo de la petición (canal interno).
 */
public record AssignApplicationAdministratorRequest(TenantId tenantId, ApplicationId applicationId, UserId userId) {

    public AssignApplicationAdministratorRequest {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
        Objects.requireNonNull(userId, RequiredArgumentMessages.USER_ID);
    }
}
