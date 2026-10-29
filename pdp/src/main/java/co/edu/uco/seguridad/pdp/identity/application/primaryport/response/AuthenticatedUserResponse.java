package co.edu.uco.seguridad.pdp.identity.application.primaryport.response;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;

/** Datos locales mínimos de una identidad autenticada; no es un DTO HTTP. */
public record AuthenticatedUserResponse(UserId userId, TenantId tenantId, String name, String email) {
}
