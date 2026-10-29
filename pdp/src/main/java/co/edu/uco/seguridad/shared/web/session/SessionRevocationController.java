package co.edu.uco.seguridad.shared.web.session;

import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.shared.auth.port.RefreshTokenStorePort;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import co.edu.uco.seguridad.shared.security.LocalUserPrincipal;
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import co.edu.uco.seguridad.shared.security.revocation.TokenRevocationPort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.Instant;
import java.util.Objects;

/**
 * Endpoint para revocar la sesión activa del usuario autenticado (BFF o Bearer) mediante TokenRevocationPort.
 */
@RestController
public final class SessionRevocationController {

    private final TokenRevocationPort tokenRevocationPort;
    private final TimeProvider timeProvider;
    private final SubjectUserIdLookupValidator subjectUserIdLookup;
    private final RefreshTokenStorePort refreshTokenStore;

    public SessionRevocationController(TokenRevocationPort tokenRevocationPort, TimeProvider timeProvider,
                                       SubjectUserIdLookupValidator subjectUserIdLookup,
                                       RefreshTokenStorePort refreshTokenStore) {
        this.tokenRevocationPort = Objects.requireNonNull(tokenRevocationPort, "tokenRevocationPort");
        this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider");
        this.subjectUserIdLookup = Objects.requireNonNull(subjectUserIdLookup, "subjectUserIdLookup");
        this.refreshTokenStore = Objects.requireNonNull(refreshTokenStore, "refreshTokenStore");
    }

    @DeleteMapping("/api/v1/session")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> revokeSession(ServerWebExchange exchange) {
        return ReactiveSecurityContextHolder.getContext()
                .map(org.springframework.security.core.context.SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .flatMap(principal -> resolveSession(principal, exchange))
                .flatMap(this::revoke);
    }

    private Mono<AuthenticatedSession> resolveSession(Object principal, ServerWebExchange exchange) {
        if (principal instanceof LocalUserPrincipal user) {
            return exchange.getSession()
                    .map(session -> new AuthenticatedSession(UserId.of(user.userId()),
                            session.getAttribute(KeycloakLogoutController.KEYCLOAK_SESSION_ID_ATTRIBUTE), null, session));
        }
        if (principal instanceof PdpPrincipal pdp) {
            return Mono.justOrEmpty(pdp.userId())
                    .switchIfEmpty(subjectUserIdLookup.execute(pdp.subject())
                            .switchIfEmpty(Mono.error(new IllegalStateException("No existe identidad local para el sujeto"))))
                    .map(userId -> new AuthenticatedSession(userId, null, null, null));
        }
        if (principal instanceof Jwt jwt) {
            return subjectUserIdLookup.execute(jwt.getSubject())
                    .switchIfEmpty(Mono.error(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)))
                    .map(userId -> new AuthenticatedSession(userId, null, jwt, null));
        }
        if (principal instanceof Principal p) {
            return Mono.fromCallable(() -> new AuthenticatedSession(UserId.of(p.getName()), null, null, null));
        }
        return Mono.error(new IllegalStateException("Principal no soportado para revocación"));
    }

    private Mono<Void> revoke(AuthenticatedSession session) {
        Instant revokedAt = timeProvider.now();
        Mono<Void> revokeCurrentToken = session.jwt() != null && session.jwt().getId() != null
                && session.jwt().getExpiresAt() != null
                ? tokenRevocationPort.revokeToken(session.userId(), session.jwt().getId(), session.jwt().getExpiresAt())
                : Mono.empty();
        if (session.jwt() != null) {
            // Canal bearer (ADR-034): acotado al cliente (azp); no toca el refresh del BFF ni la sesión web.
            String clientId = session.jwt().getClaimAsString("azp");
            Mono<Void> revokeClient = clientId == null || clientId.isBlank()
                    ? Mono.empty()
                    : Mono.defer(() -> tokenRevocationPort.revokeClientSince(session.userId(), clientId, revokedAt));
            return revokeCurrentToken.then(revokeClient);
        }
        Mono<Void> removeRefreshToken = session.sessionId() == null
                ? Mono.empty()
                : Mono.defer(() -> refreshTokenStore.delete(session.sessionId()));
        Mono<Void> invalidateBffSession = session.webSession() == null
                ? Mono.empty()
                : Mono.defer(session.webSession()::invalidate);
        return tokenRevocationPort.revokeAllSince(session.userId(), revokedAt)
                .then(revokeCurrentToken)
                .then(removeRefreshToken)
                .then(invalidateBffSession);
    }

    private record AuthenticatedSession(UserId userId, String sessionId, Jwt jwt, WebSession webSession) {
    }
}
