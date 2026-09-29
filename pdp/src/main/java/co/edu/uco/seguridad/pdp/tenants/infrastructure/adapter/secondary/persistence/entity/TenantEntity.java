package co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.secondary.persistence.entity;

/**
 * Representación de persistencia de un inquilino: plana, tipada primitivamente e inmutable.
 */
public record TenantEntity(String id, String name, String status) {
}
