package co.edu.uco.seguridad.pdp.recursos.domain;

import co.edu.uco.seguridad.crosscutting.messages.RequiredArgumentMessages;
import co.edu.uco.seguridad.pdp.commons.AggregateRoot;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.ResourceId;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.recursos.domain.event.ProtectedResourceRegistered;

import java.time.Instant;
import java.util.Objects;

/**
 * Un recurso de una aplicación protegida junto con la acción permitida sobre él. Lleva
 * {@code applicationName} desnormalizado a propósito: permite filtrar por nombre sin tocar el
 * almacenamiento de Aplicaciones.
 */
public record ProtectedResource(ResourceId id,
                                ApplicationId applicationId,
                                TenantId tenantId,
                                ApplicationName applicationName,
                                ResourceCode code,
                                ActionCode action,
                                Instant registeredAt) {

    public ProtectedResource {
        Objects.requireNonNull(id, RequiredArgumentMessages.RESOURCE_ID);
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(applicationName, RequiredArgumentMessages.APPLICATION_NAME);
        Objects.requireNonNull(code, RequiredArgumentMessages.RESOURCE_CODE);
        Objects.requireNonNull(action, RequiredArgumentMessages.ACTION_CODE);
        Objects.requireNonNull(registeredAt, RequiredArgumentMessages.REGISTERED_AT);
    }

    public static ProtectedResource register(ResourceId id,
                                             ApplicationId applicationId,
                                             TenantId tenantId,
                                             ApplicationName applicationName,
                                             ResourceCode code,
                                             ActionCode action,
                                             Instant registeredAt) {
        return new ProtectedResource(id, applicationId, tenantId, applicationName, code, action, registeredAt);
    }

    /** Igual que {@link #register}, pero empareja el recurso con su evento — usar al escribir, no al leer. */
    public static AggregateRoot<ProtectedResource, ProtectedResourceRegistered> registerWithEvent(
            ResourceId id,
            ApplicationId applicationId,
            TenantId tenantId,
            ApplicationName applicationName,
            ResourceCode code,
            ActionCode action,
            Instant registeredAt) {
        ProtectedResource resource =
                register(id, applicationId, tenantId, applicationName, code, action, registeredAt);
        return AggregateRoot.of(resource, ProtectedResourceRegistered.of(resource));
    }

    public boolean belongsTo(TenantId candidate) {
        return tenantId.equals(candidate);
    }

    /** Identidad funcional de una entrada del catálogo: el mismo triple no puede ser registrado dos veces. */
    public boolean isSameGrantAs(ApplicationId otherApplication, ResourceCode otherCode, ActionCode otherAction) {
        return applicationId.equals(otherApplication) && code.equals(otherCode) && action.equals(otherAction);
    }
}
