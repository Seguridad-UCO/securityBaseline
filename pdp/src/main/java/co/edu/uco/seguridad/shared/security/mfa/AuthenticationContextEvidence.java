package co.edu.uco.seguridad.shared.security.mfa;

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
}
