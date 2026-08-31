package co.edu.uco.seguridad.shared.auth.service;

import co.edu.uco.seguridad.pdp.identity.application.primaryport.request.ProvisionIdentityRequest;
import co.edu.uco.seguridad.pdp.identity.application.usecase.ProvisionIdentityUseCase;
import co.edu.uco.seguridad.pdp.identity.domain.Email;
import co.edu.uco.seguridad.shared.auth.model.OidcFlowIntent;
import co.edu.uco.seguridad.shared.security.LocalUserPrincipal;
import co.edu.uco.seguridad.shared.web.session.KeycloakLogoutController;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Locale;

@Component
@Profile("keycloak")
public final class OidcAuthenticationSuccessHandler implements ServerAuthenticationSuccessHandler {

    private final ProvisionIdentityUseCase provisionIdentity;
    private final OidcFlowStateService flowState;
    private final OidcRedirectPolicy redirectPolicy;
    private final KeycloakOidcSessionService oidcSessionService;
    private final WebSessionServerSecurityContextRepository securityContextRepository =
            new WebSessionServerSecurityContextRepository();

    OidcAuthenticationSuccessHandler(ProvisionIdentityUseCase provisionIdentity, OidcFlowStateService flowState,
            OidcRedirectPolicy redirectPolicy, KeycloakOidcSessionService oidcSessionService) {
        this.provisionIdentity = provisionIdentity;
        this.flowState = flowState;
        this.redirectPolicy = redirectPolicy;
        this.oidcSessionService = oidcSessionService;
    }

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange exchange, Authentication authentication) {
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        return flowState.current(exchange.getExchange())
                .flatMap(intent -> intent == OidcFlowIntent.REGISTER
                        ? restartAtKeycloakLogin(exchange.getExchange(), oidcUser.getIdToken().getTokenValue())
                        : startLocalSession(exchange.getExchange(), oidcUser));
    }

    private Mono<Void> startLocalSession(ServerWebExchange exchange, OidcUser oidc) {
        String email = oidc.getEmail();
        if (email == null || email.isBlank()) {
            return Mono.error(new IllegalArgumentException("Keycloak no entregó un correo verificable."));
        }
        String name = oidc.getFullName() == null || oidc.getFullName().isBlank() ? oidc.getGivenName() : oidc.getFullName();
        String provider = authProvider(oidc);
        ProvisionIdentityRequest request = new ProvisionIdentityRequest(oidc.getIdToken().getIssuer().toString(),
                oidc.getSubject(), new Email(email), name, provider);
        return provisionIdentity.execute(request)
                .flatMap(localUser -> saveLocalSession(exchange, localUser, oidc.getIdToken().getTokenValue()))
                .then(flowState.clear(exchange))
                .then(redirect(exchange, redirectPolicy.frontendHome()));
    }

    private Mono<Void> restartAtKeycloakLogin(ServerWebExchange exchange, String idToken) {
        return exchange.getSession()
                .flatMap(session -> session.invalidate())
                .then(oidcSessionService.logoutUri(idToken, redirectPolicy.frontendRegistrationReady()))
                .flatMap(location -> redirect(exchange, location));
    }

    private Mono<Void> saveLocalSession(ServerWebExchange exchange, LocalUserPrincipal user, String idToken) {
        var authentication = new UsernamePasswordAuthenticationToken(user, null, List.of());
        return exchange.getSession()
                .doOnNext(session -> session.getAttributes().put(KeycloakLogoutController.KEYCLOAK_ID_TOKEN_ATTRIBUTE, idToken))
                .then(securityContextRepository.save(exchange, new SecurityContextImpl(authentication)));
    }

    private Mono<Void> redirect(ServerWebExchange exchange, String location) {
        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.FOUND);
        exchange.getResponse().getHeaders().setLocation(URI.create(location));
        return exchange.getResponse().setComplete();
    }

    private static String authProvider(OidcUser oidc) {
        String broker = oidc.getClaimAsString("identity_provider");
        return broker == null || broker.isBlank() ? "keycloak-local" : broker.toLowerCase(Locale.ROOT);
    }
}
