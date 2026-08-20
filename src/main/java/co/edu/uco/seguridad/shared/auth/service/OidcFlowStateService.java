package co.edu.uco.seguridad.shared.auth.service;

import co.edu.uco.seguridad.shared.auth.model.OidcFlowIntent;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Service
public final class OidcFlowStateService {

    public static final String FLOW_INTENT_ATTRIBUTE = "OIDC_FLOW_INTENT";

    public Mono<Void> remember(ServerWebExchange exchange, OidcFlowIntent intent) {
        return exchange.getSession()
                .doOnNext(session -> session.getAttributes().put(FLOW_INTENT_ATTRIBUTE, intent.name()))
                .then();
    }

    public Mono<OidcFlowIntent> current(ServerWebExchange exchange) {
        return exchange.getSession()
                .mapNotNull(session -> session.getAttribute(FLOW_INTENT_ATTRIBUTE))
                .cast(String.class)
                .map(OidcFlowIntent::valueOf)
                .defaultIfEmpty(OidcFlowIntent.LOGIN);
    }

    public Mono<Void> clear(ServerWebExchange exchange) {
        return exchange.getSession()
                .doOnNext(session -> session.getAttributes().remove(FLOW_INTENT_ATTRIBUTE))
                .then();
    }
}
