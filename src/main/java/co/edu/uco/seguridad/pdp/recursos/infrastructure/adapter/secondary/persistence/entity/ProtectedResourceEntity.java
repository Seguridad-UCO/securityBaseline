package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.persistence.entity;

import java.time.Instant;

/**
 * Representación de persistencia de una fila de catálogo: plana, tipada primitivamente y mutable.
 */
public final class ProtectedResourceEntity {

    private final String id;
    private final String applicationId;
    private final String tenantId;
    private final String applicationName;
    private final String resourceCode;
    private final String action;
    private final Instant registeredAt;

    public ProtectedResourceEntity(String id,
                                   String applicationId,
                                   String tenantId,
                                   String applicationName,
                                   String resourceCode,
                                   String action,
                                   Instant registeredAt) {
        this.id = id;
        this.applicationId = applicationId;
        this.tenantId = tenantId;
        this.applicationName = applicationName;
        this.resourceCode = resourceCode;
        this.action = action;
        this.registeredAt = registeredAt;
    }

    public String getId() {
        return id;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getResourceCode() {
        return resourceCode;
    }

    public String getAction() {
        return action;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }
}
