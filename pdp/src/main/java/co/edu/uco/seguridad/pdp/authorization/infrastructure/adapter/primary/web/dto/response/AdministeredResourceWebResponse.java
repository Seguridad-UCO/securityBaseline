package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response;

import java.time.Instant;

/**
 * Respuesta plana (HU-017). Misma forma exacta que {@code resources...ProtectedResourceWebResponse}
 * — nombre distinto porque el endpoint vive en otro módulo, no porque el contrato HTTP cambie (mismo
 * criterio que {@code AdministeredApplicationWebResponse} en HU-015 y
 * {@code RoleAdministrationWebResponse} en HU-016).
 */
public record AdministeredResourceWebResponse(String id, String applicationId, String tenantId, String path,
                                              String method, Instant registeredAt) {
}
