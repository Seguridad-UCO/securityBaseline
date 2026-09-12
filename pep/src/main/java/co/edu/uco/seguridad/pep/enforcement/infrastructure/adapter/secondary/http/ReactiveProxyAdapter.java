package co.edu.uco.seguridad.pep.enforcement.infrastructure.adapter.secondary.http;

import co.edu.uco.seguridad.pep.commons.EnforcementFailure;
import co.edu.uco.seguridad.pep.commons.ProxyTarget;
import co.edu.uco.seguridad.pep.enforcement.ForwardHttpRequest;
import co.edu.uco.seguridad.pep.enforcement.infrastructure.properties.ProxyProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.util.concurrent.atomic.AtomicLong;

import static co.edu.uco.seguridad.pep.commons.EnforcementFailure.Kind.*;

@Component
@EnableConfigurationProperties(ProxyProperties.class)
final class ReactiveProxyAdapter implements ForwardHttpRequest {

    private final WebClient client;
    private final ProxyProperties properties;

    ReactiveProxyAdapter(ProxyProperties properties) {
        this.properties = properties;
        this.client = WebClient.builder().clientConnector(new ReactorClientHttpConnector(
                HttpClient.create().followRedirect(false)
                        .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(properties.connectTimeout().toMillis()))
                        .responseTimeout(properties.timeout()))).build();
    }

    @Override
    public Mono<Void> execute(ServerWebExchange exchange, ProxyTarget target) {
        return Mono.defer(() -> {
            String query = exchange.getRequest().getURI().getRawQuery();
            URI uri = URI.create(target.origin() + target.path() + (query == null ? "" : "?" + query));
            AtomicLong received = new AtomicLong();
            var body = exchange.getRequest().getBody().<DataBuffer>handle((buffer, sink) -> {
                if (received.addAndGet(buffer.readableByteCount()) > properties.maxBodyBytes()) {
                    DataBufferUtils.release(buffer);
                    sink.error(new EnforcementFailure(TOO_LARGE, "REQUEST_TOO_LARGE"));
                } else sink.next(buffer);
            }).doOnDiscard(DataBuffer.class, DataBufferUtils::release);
            return client.method(exchange.getRequest().getMethod()).uri(uri)
                    .headers(headers -> {
                        SafeHeaders.copy(exchange.getRequest().getHeaders(), headers, true, target.forwardBearer(), target.forwardCookies());
                        headers.set("X-Request-Id", exchange.getAttribute("pep.requestId"));
                        headers.set("X-Correlation-Id", exchange.getAttribute("pep.correlationId"));
                        headers.set("X-Decision-Id", exchange.getAttribute("pep.decisionId"));
                        // Proto derives from the actual listener; no client Forwarded headers are trusted.
                        headers.set("X-Forwarded-Proto", exchange.getRequest().getURI().getScheme());
                        if (exchange.getRequest().getRemoteAddress() != null) {
                            headers.set("X-Forwarded-For", exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
                        }
                    })
                    .body(BodyInserters.fromDataBuffers(body))
                    .exchangeToMono(response -> {
                        if (response.headers().contentType().filter(MediaType.TEXT_EVENT_STREAM::isCompatibleWith).isPresent()) {
                            return response.releaseBody().then(Mono.error(new EnforcementFailure(UNSUPPORTED, "UNSUPPORTED_STREAM")));
                        }
                        exchange.getResponse().setStatusCode(response.statusCode());
                        SafeHeaders.copy(response.headers().asHttpHeaders(), exchange.getResponse().getHeaders(),
                                false, false, target.forwardCookies());
                        return exchange.getResponse().writeWith(response.bodyToFlux(DataBuffer.class)
                                .doOnDiscard(DataBuffer.class, DataBufferUtils::release));
                    })
                    .timeout(properties.timeout())
                    .onErrorMap(error -> !(error instanceof EnforcementFailure), error -> {
                        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                            if (cause instanceof EnforcementFailure failure) return failure;
                            if (cause instanceof java.util.concurrent.TimeoutException
                                    || cause instanceof io.netty.handler.timeout.ReadTimeoutException) {
                                return new EnforcementFailure(GATEWAY_TIMEOUT, "UPSTREAM_TIMEOUT");
                            }
                        }
                        return new EnforcementFailure(BAD_GATEWAY, "UPSTREAM_UNAVAILABLE");
                    });
        });
    }
}
