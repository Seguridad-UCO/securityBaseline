package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response;

import java.util.List;

/**
 * Respuesta plana (HU-016). Misma forma exacta que {@code roles...RoleWebResponse} — nombre
 * distinto porque el endpoint vive en otro módulo, no porque el contrato HTTP cambie (mismo
 * criterio que {@code AdministeredApplicationWebResponse} en HU-015). {@code tenantId} y
 * {@code applicationId} van nulos cuando el alcance no los tiene.
 */
public record RoleAdministrationWebResponse(String id, String name, String scope, String tenantId,
                                            String applicationId, List<String> resourceIds, String registeredAt) {
}
