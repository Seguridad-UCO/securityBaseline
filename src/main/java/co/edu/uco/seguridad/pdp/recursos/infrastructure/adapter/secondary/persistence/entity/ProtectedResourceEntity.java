package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.persistence.entity;

import java.time.Instant;

/**
 * Representación de persistencia de una fila de catálogo: plana, tipada primitivamente y mutable.
 */
public final class ProtectedResourceEntity {

    private String id;
    private String applicationId;
    private String tenantId;
    private String applicationName;
    private String resourceCode;
    private String action;
    private Instant registeredAt;

    public ProtectedResourceEntity() {
        // Requerido por frameworks de persistencia.
    }

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

    public void setId(String id) {
        this.id = id;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getResourceCode() {
        return resourceCode;
    }

    public void setResourceCode(String resourceCode) {
        this.resourceCode = resourceCode;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }
}
