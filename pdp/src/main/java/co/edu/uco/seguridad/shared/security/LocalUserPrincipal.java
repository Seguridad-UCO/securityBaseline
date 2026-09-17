package co.edu.uco.seguridad.shared.security;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.security.mfa.AuthenticationContextEvidence;

/** Identidad de la sesión propia; no contiene ni expone tokens del proveedor. */
public record LocalUserPrincipal(String userId, String subject, TenantId tenantId, String email, String name,
        AuthenticationContextEvidence authenticationContext) {

    /**
     * Compatibilidad (HU-024): {@code ProvisionIdentityUseCaseImpl} no sabe de MFA y sigue
     * construyendo sin evidencia — {@code OidcAuthenticationSuccessHandler} reconstruye con la
     * evidencia real (extraída del ID token) antes de persistir la sesión (PLAN-HU-024.md §7).
     */
    public LocalUserPrincipal(String userId, String subject, TenantId tenantId, String email, String name) {
        this(userId, subject, tenantId, email, name, AuthenticationContextEvidence.NONE);
    }
}
