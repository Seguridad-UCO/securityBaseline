package co.edu.uco.seguridad.pep.ingress.infrastructure.config;

import io.micrometer.common.KeyValues;
import io.micrometer.observation.ObservationFilter;
import io.micrometer.observation.ObservationPredicate;
import io.micrometer.tracing.Tracer;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.reactive.function.client.ClientRequestObservationContext;
import org.springframework.web.reactive.function.client.DefaultClientRequestObservationConvention;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

/**
 * Capacidades técnicas; no modifica reglas ni contratos de dominio.
 */
@Configuration(proxyBeanMethods = false)
public class TelemetryConfiguration {
    @Bean
    ObservationPredicate excludeManagementTraffic() {
        return (name, context) -> {
            if (context instanceof org.springframework.http.server.reactive.observation.ServerRequestObservationContext server)
                return !server.getCarrier().getURI().getPath().startsWith("/actuator/");
            if (context instanceof ClientRequestObservationContext client && client.getCarrier() != null)
                return !client.getCarrier().build().url().getPath().startsWith("/actuator/");
            return !name.startsWith("spring.security.");
        };
    }

    @Bean
    DefaultClientRequestObservationConvention safeClientObservationConvention() {
        return new DefaultClientRequestObservationConvention() {
            @Override
            public KeyValues getHighCardinalityKeyValues(ClientRequestObservationContext context) {
                // La URI puede contener secretos en query, path o userinfo.
                return KeyValues.empty();
            }
        };
    }

    @Bean
    ObservationFilter safeTelemetryAttributes() {
        return context -> {
            context.removeHighCardinalityKeyValue("http.url");
            context.removeHighCardinalityKeyValue("url.full");
            return context;
        };
    }

    @Bean
    @Order(-200)
    WebFilter requestTelemetry(Tracer tracer) {
        var log = LoggerFactory.getLogger("security.http");
        return (exchange, chain) -> Mono.deferContextual(context -> {
            var span = tracer.currentSpan();
            String request = exchange.getAttributeOrDefault("pep.requestId", "");
            String correlation = exchange.getAttributeOrDefault("pep.correlationId", "");
            if (span != null) {
                span.tag("requestId", request);
                span.tag("correlationId", correlation);
            }
            String traceId = span == null ? "" : span.context().traceId();
            String spanId = span == null ? "" : span.context().spanId();
            long started = System.nanoTime();
            return chain.filter(exchange).doFinally(signal -> {
                if (!exchange.getRequest().getPath().value().startsWith("/actuator/")) {
                    int status = exchange.getResponse().getStatusCode() == null
                            ? 200 : exchange.getResponse().getStatusCode().value();
                    try (var traceScope = org.slf4j.MDC.putCloseable("traceId", traceId);
                         var spanScope = org.slf4j.MDC.putCloseable("spanId", spanId);
                         var requestScope = org.slf4j.MDC.putCloseable("requestId", request);
                         var correlationScope = org.slf4j.MDC.putCloseable("correlationId", correlation)) {
                        log.atInfo().addKeyValue("event.name", "http.request.completed")
                                .addKeyValue("operation", "http.server")
                                .addKeyValue("http.response.status_code", status)
                                .addKeyValue("duration_ms", (System.nanoTime() - started) / 1_000_000.0)
                                .addKeyValue("outcome", signal == reactor.core.publisher.SignalType.CANCEL
                                        ? "cancelled" : status >= 500 ? "error" : "success")
                                .log("Petición HTTP finalizada");
                    }
                }
            });
        });
    }

    @Bean
    @Profile("observability")
    @Order(-100)
    SecurityWebFilterChain telemetryManagementSecurity(ServerHttpSecurity http,
                                                       @Value("${management.server.port:9081}") int managementPort) {
        return http.securityMatcher(exchange -> exchange.getRequest().getLocalAddress() != null
                        && exchange.getRequest().getLocalAddress().getPort() == managementPort
                        ? ServerWebExchangeMatcher.MatchResult.match()
                        : ServerWebExchangeMatcher.MatchResult.notMatch())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(rules -> rules
                        .pathMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**",
                                "/actuator/prometheus").permitAll()
                        .anyExchange().denyAll())
                .build();
    }
}
