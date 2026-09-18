package co.edu.uco.seguridad.shared.security.mfa;

import org.springframework.security.oauth2.core.ClaimAccessor;

import java.util.List;
import java.util.Optional;

/**
 * Evidencia cruda de cómo se autenticó la sesión (HU-024, ADR-027) — los claims estándar OIDC
 * {@code acr}/{@code amr}, sin interpretar. No decide si satisface MFA: eso es
 * {@link MfaEvidenceProperties#satisfiedBy(AuthenticationContextEvidence)}, que sí conoce la
 * configuración (qué claim mirar, qué valores aceptar). Un {@code acr} ausente o un {@code amr}
 * vacío son estados válidos — "sin evidencia" —, no errores de formato.
 */
public record AuthenticationContextEvidence(Optional<String> acr, List<String> amr) {

    public AuthenticationContextEvidence {
    }

    /** Sin evidencia — valor por defecto para sesiones que todavía no la resuelven (HU-024 §7). */
    public static final AuthenticationContextEvidence NONE = new AuthenticationContextEvidence(Optional.empty(), List.of());

    /**
     * Lee {@code acr}/{@code amr} de cualquier token/portador de claims OIDC ({@code Jwt},
     * {@code OidcUser}, {@code OidcIdToken}) — mismo camino de extracción para los tres, sin
     * duplicar la lógica (PLAN-HU-024.md §7). {@code amr} ausente resuelve a lista vacía, no a
     * {@code null}.
     */
    public static AuthenticationContextEvidence from(ClaimAccessor claims) {
        List<String> amr = claims.getClaimAsStringList("amr");
        return new AuthenticationContextEvidence(Optional.ofNullable(claims.getClaimAsString("acr")),
                amr == null ? List.of() : amr);
    }
}
