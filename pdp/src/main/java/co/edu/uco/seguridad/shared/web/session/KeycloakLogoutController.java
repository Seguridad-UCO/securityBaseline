package co.edu.uco.seguridad.shared.web.session;

import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.auth.port.RefreshTokenStorePort;
import co.edu.uco.seguridad.shared.auth.service.KeycloakOidcSessionService;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import co.edu.uco.seguridad.shared.security.LocalUserPrincipal;
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import co.edu.uco.seguridad.shared.security.revocation.TokenRevocationPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

/**
 * Logout de navegador: invalida la sesión BFF, revoca tokens activos del sujeto y completa el logout RP-initiated en Keycloak.
 */
@Controller
@Profile("keycloak")
public final class KeycloakLogoutController {

    public static final String KEYCLOAK_ID_TOKEN_ATTRIBUTE = "KEYCLOAK_ID_TOKEN";
    public static final String KEYCLOAK_SESSION_ID_ATTRIBUTE = "KEYCLOAK_SESSION_ID";

    private final KeycloakOidcSessionService oidcSessionService;
    private final TokenRevocationPort tokenRevocationPort;
    private final RefreshTokenStorePort refreshTokenStore;
    private final TimeProvider timeProvider;
    private final String frontendOrigin;

    public KeycloakLogoutController(KeycloakOidcSessionService oidcSessionService,
                                    TokenRevocationPort tokenRevocationPort,
                                    RefreshTokenStorePort refreshTokenStore,
                                    TimeProvider timeProvider,
                                    @Value("${pdp.frontend.origin}") String frontendOrigin) {
        this.oidcSessionService = Objects.requireNonNull(oidcSessionService, "oidcSessionService");
        this.tokenRevocationPort = Objects.requireNonNull(tokenRevocationPort, "tokenRevocationPort");
        this.refreshTokenStore = Objects.requireNonNull(refreshTokenStore, "refreshTokenStore");
        this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider");
        this.frontendOrigin = Objects.requireNonNull(frontendOrigin, "frontendOrigin");
    }

    @GetMapping("/api/v1/session/logout")
    Mono<Void> logout(ServerWebExchange exchange) {
        return exchange.getSession()
                .flatMap(session -> {
                    String idToken = session.getAttribute(KEYCLOAK_ID_TOKEN_ATTRIBUTE);
                    String sessionId = session.getAttribute(KEYCLOAK_SESSION_ID_ATTRIBUTE);

                    Mono<Void> revocation = resolveUserId(session)
                            .flatMap(userId -> tokenRevocationPort.revokeAllSince(userId, timeProvider.now()))
                            .then();
                    Mono<Void> removeRefreshToken = sessionId == null
                            ? Mono.empty()
                            : refreshTokenStore.delete(sessionId);

                    return revocation
                            .then(removeRefreshToken)
                            .then(Mono.fromRunnable(() -> {
                                session.getAttributes().remove(KEYCLOAK_ID_TOKEN_ATTRIBUTE);
                                session.getAttributes().remove(KEYCLOAK_SESSION_ID_ATTRIBUTE);
                            }))
                            .then(Mono.defer(session::invalidate))
                            .then(oidcSessionService.logoutUri(idToken, frontendOrigin))
                            .flatMap(uri -> redirect(exchange, uri));
                });
    }

    private Mono<UserId> resolveUserId(WebSession session) {
        for (Object val : session.getAttributes().values()) {
            if (val instanceof org.springframework.security.core.context.SecurityContext sc && sc.getAuthentication() != null) {
                Object principal = sc.getAuthentication().getPrincipal();
                if (principal instanceof LocalUserPrincipal user) {
                    return Mono.just(new UserId(UUID.fromString(user.userId())));
                }
                if (principal instanceof PdpPrincipal pdp && pdp.userId().isPresent()) {
                    return Mono.just(pdp.userId().get());
                }
            }
        }
        return Mono.empty();
    }

    private Mono<Void> redirect(ServerWebExchange exchange, String uri) {
        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.FOUND);
        exchange.getResponse().getHeaders().setLocation(java.net.URI.create(uri));
        return exchange.getResponse().setComplete();
    }
}
