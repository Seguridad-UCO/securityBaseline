package co.edu.uco.seguridad.shared.security;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;

/** Identidad de la sesión propia; no contiene ni expone tokens del proveedor. */
public record LocalUserPrincipal(String userId, String subject, TenantId tenantId, String email, String name) { }
