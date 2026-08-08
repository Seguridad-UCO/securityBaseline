package co.edu.uco.seguridad.pdp.recursos.domain;

import co.edu.uco.seguridad.pdp.commons.AggregateRoot;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.ResourceId;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.recursos.domain.event.ProtectedResourceRegistered;

import java.time.Instant;
import java.util.Objects;

/**
 * Un recurso de una aplicación protegida junto con la acción permitida sobre él.
 *
 * <p>Inmutable y libre de tipos de framework. Lleva {@code applicationName} a propósito: el
 * catálogo es un modelo de lectura propiedad de este módulo, y desnormalizar el nombre es lo que permite
 * que una búsqueda filtre por nombre sin acceder al almacenamiento de Aplicaciones.</p>
 */
public record ProtectedResource(ResourceId id,
                                ApplicationId applicationId,
                                TenantId tenantId,
                                ApplicationName applicationName,
                                ResourceCode code,
                                ActionCode action,
                                Instant registeredAt) {

    public ProtectedResource {
        Objects.requireNonNull(id, "se requiere id de recurso");
        Objects.requireNonNull(applicationId, "se requiere id de aplicación");
        Objects.requireNonNull(tenantId, "se requiere id de inquilino");
        Objects.requireNonNull(applicationName, "se requiere nombre de aplicación");
        Objects.requireNonNull(code, "se requiere código de recurso");
        Objects.requireNonNull(action, "se requiere código de acción");
        Objects.requireNonNull(registeredAt, "se requiere instante de registro");
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

    /**
     * Igual que {@link #register}, pero empareja el recurso con el evento que su registro produce
     * (ver {@link AggregateRoot}). Es la fábrica que debe usar el caso de uso de escritura;
     * {@link #register} sigue existiendo para reconstrucción desde persistencia.
     */
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
