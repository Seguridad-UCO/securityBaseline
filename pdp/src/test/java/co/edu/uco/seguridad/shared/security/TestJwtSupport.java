package co.edu.uco.seguridad.shared.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.util.context.Context;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Construye lo que un token JWT válido produce, en dos formas: un {@link Jwt} de dominio para
 * inyectar directamente en pruebas de interactor sin contexto de Spring, y un token firmado y
 * serializado para pruebas HTTP de extremo a extremo que sí pasan por el
 * {@code ReactiveJwtDecoder} real.
 *
 * <p>El secreto y el emisor deben coincidir con {@code application.properties}
 * ({@code pdp.security.jwt.secret}/{@code .issuer}) porque las pruebas HTTP firman con este mismo
 * secreto y el decoder real de la aplicación es quien valida la firma.</p>
 */
public final class TestJwtSupport {

    public static final String SECRET = "dev-only-signing-key-not-for-production-use-please-change-1234";
    public static final String ISSUER = "https://security-baseline.pdp.local";
    public static final String AUDIENCE = "security-baseline-pdp";

    /**
     * Valor de {@code acr} que las pruebas HTTP administrativas configuran vía
     * {@code pdp.security.mfa.claim=acr} / {@code pdp.security.mfa.accepted-values} (HU-024) para
     * que {@link #signedTokenWithMfaEvidence(String, String)} satisfaga el step-up — sin este
     * claim, {@code MfaAwareApplicationAdministratorValidator} rechaza toda operación
     * administrativa (fail-closed, PLAN-HU-024.md §2 criterio 5).
     */
    public static final String MFA_ACCEPTED_ACR = "urn:mfa:otp";

    private TestJwtSupport() {
    }

    public static Jwt jwt(String tenant, String subject) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("test-token-value")
                .header("alg", "HS256")
                .subject(subject)
                .issuer(ISSUER)
                .audience(List.of(AUDIENCE))
                .claim("jti", UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("tenant", tenant)
                .build();
    }

    /**
     * Mismo JWT que {@link #jwt(String, String)}, con los claims {@code acr}/{@code amr} añadidos
     * (HU-024) — {@code acr} nulo o {@code amr} nulo/vacío omiten el claim, para simular su
     * ausencia real en el token en vez de un valor vacío presente.
     */
    public static Jwt jwtWithAuthenticationContext(String tenant, String subject, String acr, List<String> amr) {
        Instant now = Instant.now();
        var builder = Jwt.withTokenValue("test-token-value")
                .header("alg", "HS256")
                .subject(subject)
                .issuer(ISSUER)
                .audience(List.of(AUDIENCE))
                .claim("jti", UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("tenant", tenant);
        if (acr != null) builder.claim("acr", acr);
        if (amr != null && !amr.isEmpty()) builder.claim("amr", amr);
        return builder.build();
    }

    /**
     * Para {@code Mono#contextWrite}: hace que {@link SecurityContext#currentPrincipal()} resuelva.
     */
    public static Context withPrincipal(String tenant, String subject) {
        Authentication authentication = new JwtAuthenticationToken(jwt(tenant, subject));
        return ReactiveSecurityContextHolder.withAuthentication(authentication);
    }

    /**
     * Token firmado y serializado, listo para {@code Authorization: Bearer <token>}.
     */
    public static String signedToken(String tenant, String subject) {
        return signedTokenExpiringIn(tenant, subject, 300, true, true, null);
    }

    /**
     * Mismo token que {@link #signedToken(String, String)}, con el claim {@code acr} =
     * {@link #MFA_ACCEPTED_ACR} (HU-024) — para las rutas administrativas gateadas por
     * {@code MfaAwareApplicationAdministratorValidator} en las pruebas HTTP de extremo a extremo.
     */
    public static String signedTokenWithMfaEvidence(String tenant, String subject) {
        return signedTokenExpiringIn(tenant, subject, 300, true, true, MFA_ACCEPTED_ACR);
    }

    public static String expiredToken(String tenant, String subject) {
        return signedTokenExpiringIn(tenant, subject, -60, true, true, null);
    }

    /**
     * Válido en to-do lo demás, pero sin el claim {@code aud} — para probar el rechazo de audiencia.
     */
    public static String tokenWithoutAudience(String tenant, String subject) {
        return signedTokenExpiringIn(tenant, subject, 300, false, true, null);
    }

    /**
     * Válido en to-do lo demás, pero sin el claim {@code jti} — para probar el rechazo de identificador.
     */
    public static String tokenWithoutJti(String tenant, String subject) {
        return signedTokenExpiringIn(tenant, subject, 300, true, false, null);
    }

    private static String signedTokenExpiringIn(
            String tenant, String subject, long secondsFromNow, boolean withAudience, boolean withJti, String acr) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .issuer(ISSUER)
                    .claim("tenant", tenant)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(secondsFromNow)));
            if (withAudience) {
                claims.audience(AUDIENCE);
            }
            if (withJti) {
                claims.jwtID(UUID.randomUUID().toString());
            }
            if (acr != null) {
                claims.claim("acr", acr);
            }
            JWSSigner signer = new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8));
            SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims.build());
            signedJwt.sign(signer);
            return signedJwt.serialize();
        } catch (Exception cause) {
            throw new IllegalStateException("no se pudo firmar el token de prueba", cause);
        }
    }
}
