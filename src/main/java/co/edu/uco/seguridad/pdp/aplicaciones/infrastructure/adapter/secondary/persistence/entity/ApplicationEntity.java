package co.edu.uco.seguridad.pdp.aplicaciones.infrastructure.adapter.secondary.persistence.entity;

import java.time.Instant;

/**
 * Representación de persistencia de una aplicación: plana, tipada primitivamente y mutable, porque eso es lo que
 * los motores de almacenamiento y sus controladores requieren.
 *
 * <p>Deliberadamente no valida nada. Mantenerlo simple es lo que permite que la entidad de dominio
 * sea inmutable y siempre válida, y permite que el esquema evolucione sin tocar el modelo.</p>
 */
public final class ApplicationEntity {

    private String id;
    private String tenantId;
    private String name;
    private Instant registeredAt;

    public ApplicationEntity() {
        // Requerido por frameworks de persistencia.
    }

    public ApplicationEntity(String id, String tenantId, String name, Instant registeredAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.registeredAt = registeredAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }
}
