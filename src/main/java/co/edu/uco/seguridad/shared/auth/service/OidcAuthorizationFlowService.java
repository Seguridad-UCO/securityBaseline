package co.edu.uco.seguridad.shared.auth.service;

import co.edu.uco.seguridad.shared.auth.model.OidcFlowIntent;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerAuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

@Service
public final class OidcAuthorizationFlowService {

    private static final String REGISTRATION_FLOW_PARAMETER = "kc_action";
    private static final String REGISTRATION_FLOW_VALUE = "register";

    private final DefaultServerOAuth2AuthorizationRequestResolver loginResolver;
    private final DefaultServerOAuth2AuthorizationRequestResolver registerResolver;
    private final ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository;
    private final OidcFlowStateService flowState;

    OidcAuthorizationFlowService(ReactiveClientRegistrationRepository clientRegistrations,
            ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository,
            OidcFlowStateService flowState) {
        this.authorizationRequestRepository = authorizationRequestRepository;
        this.flowState = flowState;
        this.loginResolver = new DefaultServerOAuth2AuthorizationRequestResolver(clientRegistrations,
                new PathPatternParserServerWebExchangeMatcher("/oauth2/authorization/{registrationId}", HttpMethod.GET));
        this.registerResolver = new DefaultServerOAuth2AuthorizationRequestResolver(clientRegistrations,
                new PathPatternParserServerWebExchangeMatcher("/oauth2/authorization/{registrationId}/register", HttpMethod.GET));
    }

    public Mono<Void> beginLogin(ServerWebExchange exchange) {
        return authorize(exchange, OidcFlowIntent.LOGIN, loginResolver.resolve(exchange));
    }

    public Mono<Void> beginRegistration(ServerWebExchange exchange) {
        return authorize(exchange, OidcFlowIntent.REGISTER, registerResolver.resolve(exchange));
    }

    private Mono<Void> authorize(ServerWebExchange exchange, OidcFlowIntent intent,
            Mono<OAuth2AuthorizationRequest> authorizationRequestMono) {
        return authorizationRequestMono
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Client registration not found")))
                .map(request -> customize(request, intent, exchange))
                .flatMap(request -> flowState.remember(exchange, intent)
                        .then(authorizationRequestRepository.saveAuthorizationRequest(request, exchange))
                        .then(redirect(exchange, request.getAuthorizationRequestUri())));
    }

    private OAuth2AuthorizationRequest customize(OAuth2AuthorizationRequest request, OidcFlowIntent intent,
            ServerWebExchange exchange) {
        OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.from(request);
        builder.attributes(attributes -> attributes.put(OidcFlowStateService.FLOW_INTENT_ATTRIBUTE, intent.name()));
        if (intent == OidcFlowIntent.REGISTER) {
            builder.additionalParameters(parameters -> parameters.put(REGISTRATION_FLOW_PARAMETER, REGISTRATION_FLOW_VALUE));
        }
        String prompt = exchange.getRequest().getQueryParams().getFirst("prompt");
        if (StringUtils.hasText(prompt)) {
            builder.additionalParameters(parameters -> parameters.put("prompt", prompt));
        }
        return builder.build();
    }

    private Mono<Void> redirect(ServerWebExchange exchange, String uri) {
        exchange.getResponse().setStatusCode(HttpStatus.FOUND);
        exchange.getResponse().getHeaders().setLocation(URI.create(uri));
        return exchange.getResponse().setComplete();
    }
}
