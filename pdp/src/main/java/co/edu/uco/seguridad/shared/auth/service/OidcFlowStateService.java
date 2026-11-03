package co.edu.uco.seguridad.shared.auth.service;

import co.edu.uco.seguridad.shared.auth.model.OidcFlowIntent;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Service
public final class OidcFlowStateService {

    public static final String FLOW_INTENT_ATTRIBUTE = "OIDC_FLOW_INTENT";
    public static final String RETURN_TARGET_ATTRIBUTE = "OIDC_RETURN_TARGET";

    public Mono<Void> remember(ServerWebExchange exchange, OidcFlowIntent intent) {
        return remember(exchange, intent, null);
    }

    public Mono<Void> remember(ServerWebExchange exchange, OidcFlowIntent intent, String returnTarget) {
        return exchange.getSession()
                .doOnNext(session -> {
                    session.getAttributes().put(FLOW_INTENT_ATTRIBUTE, intent.name());
                    if (returnTarget == null) session.getAttributes().remove(RETURN_TARGET_ATTRIBUTE);
                    else session.getAttributes().put(RETURN_TARGET_ATTRIBUTE, returnTarget);
                })
                .then();
    }

    public Mono<OidcFlowIntent> current(ServerWebExchange exchange) {
        return exchange.getSession()
                .mapNotNull(session -> session.getAttribute(FLOW_INTENT_ATTRIBUTE))
                .cast(String.class)
                .map(OidcFlowIntent::valueOf)
                .defaultIfEmpty(OidcFlowIntent.LOGIN);
    }

    public Mono<String> returnTarget(ServerWebExchange exchange) {
        return exchange.getSession()
                .mapNotNull(session -> session.getAttribute(RETURN_TARGET_ATTRIBUTE))
                .cast(String.class);
    }

    public Mono<Void> clear(ServerWebExchange exchange) {
        return exchange.getSession()
                .doOnNext(session -> {
                    session.getAttributes().remove(FLOW_INTENT_ATTRIBUTE);
                    session.getAttributes().remove(RETURN_TARGET_ATTRIBUTE);
                })
                .then();
    }
}
