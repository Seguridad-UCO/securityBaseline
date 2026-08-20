package co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.dto.response;

/**
 * El contrato HTTP de salida, propiedad del adaptador web. Plano y primitivo a propósito: publicar
 * los value objects directamente haría que renombrar un campo del dominio rompiera la API en silencio.
 */
public record TenantWebResponse(String code, String name, String status) {
}
