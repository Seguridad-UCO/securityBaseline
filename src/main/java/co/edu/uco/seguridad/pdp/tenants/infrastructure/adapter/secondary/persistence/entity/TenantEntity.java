package co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.secondary.persistence.entity;

/**
 * Representación de persistencia de un inquilino: plana, tipada primitivamente y mutable, porque eso es lo que
 * los motores de almacenamiento y sus controladores requieren.
 *
 * <p>Deliberadamente no valida nada. Mantenerlo simple es lo que permite que la entidad de dominio
 * sea inmutable y siempre válida, y permite que el esquema evolucione sin tocar el modelo.</p>
 */
public final class TenantEntity {

    private String id;
    private String status;

    public TenantEntity() {
        // Requerido por frameworks de persistencia.
    }

    public TenantEntity(String id, String status) {
        this.id = id;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
