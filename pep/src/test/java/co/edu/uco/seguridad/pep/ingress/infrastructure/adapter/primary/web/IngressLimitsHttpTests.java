package co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web;

import co.edu.uco.seguridad.pep.ingress.infrastructure.properties.IngressProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServer;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class IngressLimitsHttpTests {
    private final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    private IngressLimitsFilter filter(int concurrent, int rate, int burst) {
        var props = new IngressProperties(List.of(new IngressProperties.Route("/app", "app", "test",
                URI.create("http://127.0.0.1:12345"), List.of("app"), false, false)),
                "http://localhost", URI.create("http://localhost/jwks"), true, List.of(),
                concurrent, rate, burst, 10, 1024, Duration.ofSeconds(1));
        return new IngressLimitsFilter(props, new ProblemWriter(new tools.jackson.databind.ObjectMapper()), new SimpleMeterRegistry());
    }
    @Test void concurrency_rejects_and_client_cancellation_releases_slot() throws Exception {
        AtomicInteger admitted = new AtomicInteger();
        var handler = WebHttpHandlerBuilder.webHandler(exchange -> {
            admitted.incrementAndGet();
            return exchange.getRequest().getPath().value().equals("/slow")
                    ? Mono.delay(Duration.ofSeconds(10)).then(exchange.getResponse().setComplete())
                    : exchange.getResponse().setComplete();
        }).filter(filter(1, 1000, 1000)).build();
        var server = HttpServer.create().host("127.0.0.1").port(0).handle(new ReactorHttpHandlerAdapter(handler)).bindNow();
        try {
            String origin = "http://127.0.0.1:" + server.port();
            var pending = client.sendAsync(HttpRequest.newBuilder(URI.create(origin + "/slow")).GET().build(), HttpResponse.BodyHandlers.discarding());
            await().atMost(Duration.ofSeconds(3)).until(() -> admitted.get() == 1);
            assertThat(client.send(HttpRequest.newBuilder(URI.create(origin + "/fast")).GET().build(),
                    HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(503);
            pending.cancel(true);
            await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                    assertThat(client.send(HttpRequest.newBuilder(URI.create(origin + "/fast")).GET().build(),
                            HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(200));
        } finally { server.disposeNow(); }
    }
    @Test void spoofed_forwarded_ip_cannot_evade_rate_limit() throws Exception {
        AtomicInteger admitted = new AtomicInteger();
        var handler = WebHttpHandlerBuilder.webHandler(exchange -> {
            admitted.incrementAndGet(); return exchange.getResponse().setComplete();
        }).filter(filter(10, 1, 1)).build();
        var server = HttpServer.create().host("127.0.0.1").port(0).handle(new ReactorHttpHandlerAdapter(handler)).bindNow();
        try {
            URI uri = URI.create("http://127.0.0.1:" + server.port() + "/fast");
            assertThat(client.send(HttpRequest.newBuilder(uri).header("X-Forwarded-For", "one").GET().build(),
                    HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(200);
            var limited = client.send(HttpRequest.newBuilder(uri).header("X-Forwarded-For", "two").GET().build(),
                    HttpResponse.BodyHandlers.discarding());
            assertThat(limited.statusCode()).isEqualTo(429);
            assertThat(limited.headers().firstValue("Retry-After")).contains("1");
            assertThat(admitted.get()).isEqualTo(1);
        } finally { server.disposeNow(); }
    }
}

