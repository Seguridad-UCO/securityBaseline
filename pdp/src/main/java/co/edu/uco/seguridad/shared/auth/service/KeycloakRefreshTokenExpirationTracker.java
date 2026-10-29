package co.edu.uco.seguridad.shared.auth.service;

import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.stereotype.Component;

import co.edu.uco.seguridad.shared.port.TimeProvider;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Profile("keycloak")
public final class KeycloakRefreshTokenExpirationTracker {

    private static final String REFRESH_EXPIRES_IN = "refresh_expires_in";
    private static final Duration PENDING_ENTRY_LIFETIME = Duration.ofMinutes(2);

    private final ConcurrentMap<String, PendingExpiration> pendingExpirations = new ConcurrentHashMap<>();
    private final TimeProvider timeProvider;

    public KeycloakRefreshTokenExpirationTracker(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    void record(OAuth2AccessTokenResponse response) {
        Instant now = timeProvider.now();
        removeExpiredEntries(now);
        OAuth2RefreshToken refreshToken = response.getRefreshToken();
        if (refreshToken == null) {
            return;
        }

        long expiresIn = refreshExpiresIn(response);
        Instant expiresAt = refreshToken.getIssuedAt().plusSeconds(expiresIn);
        if (!expiresAt.isAfter(now)) {
            throw new IllegalStateException("Keycloak returned an expired refresh token.");
        }
        pendingExpirations.put(fingerprint(refreshToken.getTokenValue()),
                new PendingExpiration(expiresAt, now.plus(PENDING_ENTRY_LIFETIME)));
    }

    Instant consumeExpiration(String refreshTokenValue) {
        Instant now = timeProvider.now();
        removeExpiredEntries(now);
        PendingExpiration pending = pendingExpirations.remove(fingerprint(refreshTokenValue));
        if (pending == null) {
            throw new IllegalStateException("Keycloak did not provide a refresh token expiration.");
        }
        if (!pending.expiresAt().isAfter(now)) {
            throw new IllegalStateException("Keycloak returned an expired refresh token.");
        }
        return pending.expiresAt();
    }

    private void removeExpiredEntries(Instant now) {
        pendingExpirations.entrySet().removeIf(entry -> !entry.getValue().retainedUntil().isAfter(now));
    }

    private static long refreshExpiresIn(OAuth2AccessTokenResponse response) {
        Object value = response.getAdditionalParameters().get(REFRESH_EXPIRES_IN);
        if (!(value instanceof Number number)) {
            throw new IllegalStateException("Keycloak did not provide refresh_expires_in.");
        }
        try {
            long seconds = new BigDecimal(number.toString()).longValueExact();
            if (seconds > 0) {
                return seconds;
            }
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IllegalStateException("Keycloak provided an invalid refresh_expires_in.", exception);
        }
        throw new IllegalStateException("Keycloak provided an invalid refresh_expires_in.");
    }

    private static String fingerprint(String tokenValue) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(tokenValue.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private record PendingExpiration(Instant expiresAt, Instant retainedUntil) {
    }
}
