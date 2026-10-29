package co.edu.uco.seguridad.shared.security.mfa;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

/**
 * Qué claim mirar (\"acr\" o \"amr\") y qué valores cuentan como evidencia de MFA (HU-024, ADR-027).
 *
 * <p><b>A propósito, sin invariante que bloquee el arranque.</b> A diferencia del resto de
 * {@code @ConfigurationProperties} del proyecto (p. ej. {@code RevocationRetentionProperties}), este
 * record NO valida {@code claim}/{@code acceptedValues} con {@code Objects.requireNonNull} en su
 * constructor compacto, ni el implementador debe agregarlo. Un {@code claim}/{@code acceptedValues}
 * sin configurar es un estado operativo válido y esperado antes de que el realm de Keycloak tenga el
 * flujo de MFA configurado (prerrequisito externo documentado en ADR-027 y en PLAN-HU-024.md §7) —
 * {@link #satisfiedBy(AuthenticationContextEvidence)} debe resolver {@code false} en ese caso
 * (fail-closed), no lanzar al arrancar el contexto de Spring.</p>
 */
@ConfigurationProperties(prefix = "pdp.security.mfa")
public record MfaEvidenceProperties(boolean enabled, String claim, Set<String> acceptedValues) {

    public boolean satisfiedBy(AuthenticationContextEvidence evidence) {
        if (!enabled) {
            return true;
        }
        if (claim == null || claim.isBlank() || acceptedValues == null || acceptedValues.isEmpty()) {
            return false;
        }
        return switch (claim) {
            case "acr" -> evidence.acr().filter(acceptedValues::contains).isPresent();
            case "amr" -> evidence.amr().stream().anyMatch(acceptedValues::contains);
            default -> false;
        };
    }
}
