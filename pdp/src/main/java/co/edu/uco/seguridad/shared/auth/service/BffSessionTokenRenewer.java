package co.edu.uco.seguridad.shared.auth.service;

import co.edu.uco.seguridad.shared.auth.model.BffSessionExpiredException;
import co.edu.uco.seguridad.shared.auth.model.TokenRenewalUnavailableException;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.ClientAuthorizationException;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Entrega el access token de la sesión BFF renovándolo si está por vencer (HU-030, ADR-033 §2).
 * Single-flight por {@code WebSession.getId()} sin bloquear. Solo existe con el perfil {@code keycloak}
 * (se registra en {@code SessionTokenRenewalConfiguration}).
 */
public final class BffSessionTokenRenewer {

    private static final String REGISTRATION_ID = "keycloak";
    private static final Set<String> SESSION_ENDED_CODES = Set.of("invalid_grant", "invalid_token");
    private static final String SESSION_EXPIRED_MESSAGE = "La sesión BFF ya no puede entregar un token vigente.";
    private static final String UNAVAILABLE_MESSAGE = "No se pudo renovar el token de la sesión BFF.";

    private final ReactiveOAuth2AuthorizedClientManager clientManager;
    private final TimeProvider timeProvider;
    private final Duration renewalTimeout;
    private final ConcurrentMap<String, Mono<String>> inFlight = new ConcurrentHashMap<>();

    public BffSessionTokenRenewer(ReactiveOAuth2AuthorizedClientManager clientManager,
                                  TimeProvider timeProvider, Duration renewalTimeout) {
        this.clientManager = Objects.requireNonNull(clientManager, "clientManager");
        this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider");
        this.renewalTimeout = Objects.requireNonNull(renewalTimeout, "renewalTimeout");
    }

    /**
     * @param principal principal del cliente autorizado (el {@code sub} de Keycloak)
     * @return el access token vigente
     * @throws co.edu.uco.seguridad.shared.auth.model.BffSessionExpiredException       (en el Mono) → 401
     * @throws co.edu.uco.seguridad.shared.auth.model.TokenRenewalUnavailableException (en el Mono) → 503
     */
    public Mono<String> validAccessToken(ServerWebExchange exchange, Authentication principal) {
        return Mono.defer(() -> {
            // Excepción deliberada a "sin subscribe() en la cadena": una vez Keycloak rota el refresh token,
            // la rotación DEBE guardarse en la sesión aunque el PEP corte la conexión. Por eso la renovación
            // arranca al suscribirse (sin esperar demanda) y no depende de que el suscriptor siga vivo; el
            // resultado se reparte por un Sinks.One que reproduce valor o error a cada suscriptor.
            Sinks.One<String> result = Sinks.one();
            exchange.getSession()
                    .map(WebSession::getId)
                    .flatMap(sessionId -> singleFlight(sessionId, exchange, principal))
                    .subscribe(result::tryEmitValue, result::tryEmitError, result::tryEmitEmpty);
            return result.asMono();
        });
    }

    private Mono<String> singleFlight(String sessionId, ServerWebExchange exchange, Authentication principal) {
        return inFlight.computeIfAbsent(sessionId, id -> {
            AtomicReference<Mono<String>> self = new AtomicReference<>();
            Mono<String> shared = Mono.defer(() -> authorize(exchange, principal))
                    .timeout(renewalTimeout)
                    .onErrorMap(BffSessionTokenRenewer::translate)
                    // La entrada se libera ANTES de propagar el término (doFinally corre después): una petición
                    // inmediata posterior no puede encontrar el resultado ya cacheado de la renovación anterior.
                    .doOnTerminate(() -> inFlight.remove(id, self.get()))
                    .doOnCancel(() -> inFlight.remove(id, self.get()))
                    .cache();
            self.set(shared);
            return shared;
        });
    }

    private Mono<String> authorize(ServerWebExchange exchange, Authentication principal) {
        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.withClientRegistrationId(REGISTRATION_ID)
                .principal(principal)
                .attribute(ServerWebExchange.class.getName(), exchange)
                .build();
        return clientManager.authorize(request)
                .switchIfEmpty(Mono.error(() -> new BffSessionExpiredException(SESSION_EXPIRED_MESSAGE, null)))
                .flatMap(this::stillValid);
    }

    private Mono<String> stillValid(OAuth2AuthorizedClient client) {
        OAuth2AccessToken accessToken = client.getAccessToken();
        Instant expiresAt = accessToken.getExpiresAt();
        if (expiresAt != null && !expiresAt.isAfter(timeProvider.now())) {
            return Mono.error(new BffSessionExpiredException(SESSION_EXPIRED_MESSAGE, null));
        }
        return Mono.just(accessToken.getTokenValue());
    }

    private static Throwable translate(Throwable error) {
        if (error instanceof BffSessionExpiredException || error instanceof TokenRenewalUnavailableException) {
            return error;
        }
        if (error instanceof ClientAuthorizationRequiredException) {
            return new BffSessionExpiredException(SESSION_EXPIRED_MESSAGE, error);
        }
        if (error instanceof ClientAuthorizationException authorization
                && SESSION_ENDED_CODES.contains(authorization.getError().getErrorCode())) {
            return new BffSessionExpiredException(SESSION_EXPIRED_MESSAGE, error);
        }
        return new TokenRenewalUnavailableException(UNAVAILABLE_MESSAGE, error);
    }
}
