package co.edu.uco.seguridad.shared.web.session;

import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.shared.auth.port.TokenRefreshPort;
import co.edu.uco.seguridad.shared.security.revocation.TokenRevocationPort;
import co.edu.uco.seguridad.shared.web.session.dto.TokenRefreshResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Endpoint para que aplicaciones externas renueven su token actual contra la plataforma sin exponer el refresh token.
 */
@RestController
@Profile("keycloak")
public final class TokenRefreshController {

    private final TokenRefreshPort tokenRefreshPort;
    private final TokenRevocationPort tokenRevocationPort;
    private final SubjectUserIdLookupValidator subjectUserIdLookup;

    public TokenRefreshController(TokenRefreshPort tokenRefreshPort,
                                  TokenRevocationPort tokenRevocationPort,
                                  SubjectUserIdLookupValidator subjectUserIdLookup) {
        this.tokenRefreshPort = Objects.requireNonNull(tokenRefreshPort, "tokenRefreshPort");
        this.tokenRevocationPort = Objects.requireNonNull(tokenRevocationPort, "tokenRevocationPort");
        this.subjectUserIdLookup = Objects.requireNonNull(subjectUserIdLookup, "subjectUserIdLookup");
    }

    @PostMapping("/api/v1/session/token/refresh")
    public Mono<TokenRefreshResponse> refresh() {
        return ReactiveSecurityContextHolder.getContext()
                .map(org.springframework.security.core.context.SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .flatMap(principal -> {
                    if (!(principal instanceof Jwt jwt)) {
                        return Mono.error(sessionExpired());
                    }
                    String sessionId = jwt.getClaimAsString("sid");
                    if (sessionId == null || sessionId.isBlank() || jwt.getId() == null || jwt.getExpiresAt() == null) {
                        return Mono.error(sessionExpired());
                    }
                    return subjectUserIdLookup.execute(jwt.getSubject())
                            .switchIfEmpty(Mono.error(sessionExpired()))
                            .flatMap(userId -> tokenRefreshPort.refreshToken(sessionId)
                                    .flatMap(result -> tokenRevocationPort.revokeToken(userId, jwt.getId(), jwt.getExpiresAt())
                                            .thenReturn(new TokenRefreshResponse(
                                                    result.accessToken(), result.expiresIn(), result.tokenType()))));
                });
    }

    private static ResponseStatusException sessionExpired() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "SESSION_EXPIRED");
    }
}
