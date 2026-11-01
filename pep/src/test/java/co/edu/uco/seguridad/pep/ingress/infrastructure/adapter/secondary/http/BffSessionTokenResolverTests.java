package co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.secondary.http;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import co.edu.uco.seguridad.pep.commons.EnforcementFailure;
import co.edu.uco.seguridad.pep.ingress.application.port.secondary.PdpServiceTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-031: el resolvedor distingue «sesión muerta» (401) de «canal BFF no disponible» (503).
 */
class BffSessionTokenResolverTests {
    private static final String COOKIE = "SECURITY_BASELINE_SESSION=session-123";
    private final PdpServiceTokenProvider technical = () -> Mono.just("pep-evidence-jwt");
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final Logger resolverLogger = (Logger) LoggerFactory.getLogger(BffSessionTokenResolver.class);

    @BeforeEach
    void attachLogAppender() {
        logs.start();
        resolverLogger.addAppender(logs);
    }

    @AfterEach
    void detachLogAppender() {
        resolverLogger.detachAppender(logs);
        logs.stop();
    }

    @Test
    void completes_empty_without_calling_the_pdp_when_there_is_no_bff_cookie() {
        AtomicInteger calls = new AtomicInteger();
        ExchangeFunction exchange = request -> {
            calls.incrementAndGet();
            return Mono.just(ClientResponse.create(HttpStatus.OK).build());
        };

        StepVerifier.create(resolver(exchange).resolve(exchange(null))).verifyComplete();
        StepVerifier.create(resolver(exchange).resolve(exchange("OTHER_COOKIE=value"))).verifyComplete();

        assertThat(calls).hasValue(0);
    }

    @Test
    void emits_the_access_token_and_forwards_the_cookie_over_the_technical_channel() {
        AtomicReference<URI> url = new AtomicReference<>();
        AtomicReference<HttpHeaders> headers = new AtomicReference<>();
        ExchangeFunction exchange = request -> {
            url.set(request.url());
            headers.set(request.headers());
            return Mono.just(json(HttpStatus.OK, "{\"accessToken\":\"user-access-jwt\"}"));
        };

        StepVerifier.create(resolver(exchange).resolve(exchange(COOKIE)))
                .expectNext("user-access-jwt")
                .verifyComplete();

        assertThat(url.get().getPath()).isEqualTo("/internal/v1/bff-session-token");
        assertThat(headers.get().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer pep-evidence-jwt");
        assertThat(headers.get().getFirst(HttpHeaders.COOKIE)).isEqualTo(COOKIE);
    }

    @Test
    void maps_a_pdp_401_to_unauthenticated_token_invalid() {
        StepVerifier.create(resolver(request -> Mono.just(ClientResponse.create(HttpStatus.UNAUTHORIZED).build()))
                        .resolve(exchange(COOKIE)))
                .expectErrorSatisfies(error -> assertFailure(error, EnforcementFailure.Kind.UNAUTHENTICATED, "TOKEN_INVALID"))
                .verify();
    }

    @Test
    void maps_a_pdp_401_with_www_authenticate_to_unauthenticated_token_invalid() {
        StepVerifier.create(resolver(request -> Mono.just(ClientResponse.create(HttpStatus.UNAUTHORIZED)
                                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer").build()))
                        .resolve(exchange(COOKIE)))
                .expectErrorSatisfies(error -> assertFailure(error, EnforcementFailure.Kind.UNAUTHENTICATED, "TOKEN_INVALID"))
                .verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 502, 503, 504})
    void maps_pdp_server_errors_to_bff_session_unavailable(int status) {
        StepVerifier.create(resolver(request -> Mono.just(ClientResponse.create(HttpStatusCode.valueOf(status)).build()))
                        .resolve(exchange(COOKIE)))
                .expectErrorSatisfies(error -> assertFailure(error, EnforcementFailure.Kind.UNAVAILABLE, "BFF_SESSION_UNAVAILABLE"))
                .verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 403, 404, 429})
    void maps_pdp_client_errors_other_than_401_to_bff_session_unavailable_and_logs_the_status(int status) {
        StepVerifier.create(resolver(request -> Mono.just(ClientResponse.create(HttpStatusCode.valueOf(status)).build()))
                        .resolve(exchange(COOKIE)))
                .expectErrorSatisfies(error -> assertFailure(error, EnforcementFailure.Kind.UNAVAILABLE, "BFF_SESSION_UNAVAILABLE"))
                .verify();

        assertThat(logs.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(render(event)).contains("pep.bff-session.pdp-rejected").contains(Integer.toString(status));
        });
    }

    @Test
    void maps_a_generic_exchange_error_to_bff_session_unavailable() {
        StepVerifier.create(resolver(request -> Mono.error(new IllegalStateException("network down")))
                        .resolve(exchange(COOKIE)))
                .expectErrorSatisfies(error -> assertFailure(error, EnforcementFailure.Kind.UNAVAILABLE, "BFF_SESSION_UNAVAILABLE"))
                .verify();
    }

    @Test
    void maps_a_network_or_timeout_failure_to_bff_session_unavailable() {
        var timeout = new WebClientRequestException(new TimeoutException("response timeout"), HttpMethod.GET,
                URI.create("http://pdp.local/internal/v1/bff-session-token"), HttpHeaders.EMPTY);

        StepVerifier.create(resolver(request -> Mono.error(timeout)).resolve(exchange(COOKIE)))
                .expectErrorSatisfies(error -> assertFailure(error, EnforcementFailure.Kind.UNAVAILABLE, "BFF_SESSION_UNAVAILABLE"))
                .verify();
    }

    @Test
    void fails_closed_when_a_200_has_an_empty_body() {
        StepVerifier.create(resolver(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                                .header(HttpHeaders.CONTENT_TYPE, "application/json").build()))
                        .resolve(exchange(COOKIE)))
                .expectErrorSatisfies(error -> assertFailure(error, EnforcementFailure.Kind.UNAVAILABLE, "BFF_SESSION_UNAVAILABLE"))
                .verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"accessToken\":\"   \"}", "{\"accessToken\":\"\"}", "{}", "{\"accessToken\":"})
    void fails_closed_when_a_200_has_a_blank_or_malformed_token_body(String body) {
        StepVerifier.create(resolver(request -> Mono.just(json(HttpStatus.OK, body))).resolve(exchange(COOKIE)))
                .expectErrorSatisfies(error -> assertFailure(error, EnforcementFailure.Kind.UNAVAILABLE, "BFF_SESSION_UNAVAILABLE"))
                .verify();
    }

    @Test
    void propagates_a_technical_identity_failure_without_reclassifying_it_or_calling_the_pdp() {
        AtomicInteger calls = new AtomicInteger();
        PdpServiceTokenProvider failing = () -> Mono.error(new EnforcementFailure(
                EnforcementFailure.Kind.UNAVAILABLE, "PDP_SERVICE_IDENTITY_UNAVAILABLE"));
        ExchangeFunction exchange = request -> {
            calls.incrementAndGet();
            return Mono.just(ClientResponse.create(HttpStatus.OK).build());
        };

        StepVerifier.create(new BffSessionTokenResolver(WebClient.builder().exchangeFunction(exchange).build(), failing)
                        .resolve(exchange(COOKIE)))
                .expectErrorSatisfies(error -> assertFailure(error, EnforcementFailure.Kind.UNAVAILABLE,
                        "PDP_SERVICE_IDENTITY_UNAVAILABLE"))
                .verify();
        assertThat(calls).hasValue(0);
    }

    private BffSessionTokenResolver resolver(ExchangeFunction exchange) {
        return new BffSessionTokenResolver(WebClient.builder().exchangeFunction(exchange).build(), technical);
    }

    private static ServerWebExchange exchange(String cookie) {
        MockServerHttpRequest.BaseBuilder<?> request = MockServerHttpRequest.get("/apps/demo/resource");
        if (cookie != null) request.header(HttpHeaders.COOKIE, cookie);
        return MockServerWebExchange.from(request);
    }

    private static ClientResponse json(HttpStatus status, String body) {
        return ClientResponse.create(status).header(HttpHeaders.CONTENT_TYPE, "application/json").body(body).build();
    }

    private static String render(ILoggingEvent event) {
        StringBuilder text = new StringBuilder(event.getFormattedMessage());
        if (event.getKeyValuePairs() != null) {
            event.getKeyValuePairs().forEach(pair -> text.append(' ').append(pair.key).append('=').append(pair.value));
        }
        return text.toString();
    }

    private static void assertFailure(Throwable error, EnforcementFailure.Kind kind, String code) {
        assertThat(error).isInstanceOf(EnforcementFailure.class);
        assertThat(((EnforcementFailure) error).kind()).isEqualTo(kind);
        assertThat(((EnforcementFailure) error).code()).isEqualTo(code);
    }
}
