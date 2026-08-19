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

    /** Para {@code Mono#contextWrite}: hace que {@link SecurityContext#currentPrincipal()} resuelva. */
    public static Context withPrincipal(String tenant, String subject) {
        Authentication authentication = new JwtAuthenticationToken(jwt(tenant, subject));
        return ReactiveSecurityContextHolder.withAuthentication(authentication);
    }

    /** Token firmado y serializado, listo para {@code Authorization: Bearer <token>}. */
    public static String signedToken(String tenant, String subject) {
        return signedTokenExpiringIn(tenant, subject, 300, true, true);
    }

    public static String expiredToken(String tenant, String subject) {
        return signedTokenExpiringIn(tenant, subject, -60, true, true);
    }

    /** Válido en to-do lo demás, pero sin el claim {@code aud} — para probar el rechazo de audiencia. */
    public static String tokenWithoutAudience(String tenant, String subject) {
        return signedTokenExpiringIn(tenant, subject, 300, false, true);
    }

    /** Válido en to-do lo demás, pero sin el claim {@code jti} — para probar el rechazo de identificador. */
    public static String tokenWithoutJti(String tenant, String subject) {
        return signedTokenExpiringIn(tenant, subject, 300, true, false);
    }

    private static String signedTokenExpiringIn(
            String tenant, String subject, long secondsFromNow, boolean withAudience, boolean withJti) {
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
            JWSSigner signer = new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8));
            SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims.build());
            signedJwt.sign(signer);
            return signedJwt.serialize();
        } catch (Exception cause) {
            throw new IllegalStateException("no se pudo firmar el token de prueba", cause);
        }
    }
}
