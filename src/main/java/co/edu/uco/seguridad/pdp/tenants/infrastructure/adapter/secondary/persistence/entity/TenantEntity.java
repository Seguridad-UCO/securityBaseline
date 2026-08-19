package co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.secondary.persistence.entity;

/** Representación de persistencia de un inquilino: plana, tipada primitivamente e inmutable. */
public final class TenantEntity {

    private final String id;
    private final String status;

    public TenantEntity(String id, String status) {
        this.id = id;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }
}
