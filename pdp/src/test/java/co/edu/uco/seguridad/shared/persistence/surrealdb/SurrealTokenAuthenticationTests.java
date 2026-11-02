package co.edu.uco.seguridad.shared.persistence.surrealdb;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class SurrealTokenAuthenticationTests {

    private static final Instant NOW = Instant.parse("2026-11-02T12:00:00Z");
    private static final SurrealDbProperties PROPERTIES =
            new SurrealDbProperties("http://surreal.test", "pdp", "pdp", "root", "root");

    private final AtomicInteger signins = new AtomicInteger();
    private final AtomicReference<Instant> clock = new AtomicReference<>(NOW);
    private final List<String> authorizationHeaders = new CopyOnWriteArrayList<>();

    @Test
    void every_query_carries_the_session_token_and_never_the_password() {
        SurrealTokenAuthentication filter = filter(signinReturning(Duration.ofHours(1)));

        StepVerifier.create(filter.filter(query(), recording(HttpStatus.OK))).expectNextCount(1).verifyComplete();

        assertThat(authorizationHeaders).containsExactly("Bearer token-1");
    }

    @Test
    void the_token_is_reused_while_it_is_valid() {
        SurrealTokenAuthentication filter = filter(signinReturning(Duration.ofHours(1)));

        for (int i = 0; i < 5; i++) {
            StepVerifier.create(filter.filter(query(), recording(HttpStatus.OK))).expectNextCount(1).verifyComplete();
        }

        assertThat(signins).hasValue(1);
        assertThat(authorizationHeaders).hasSize(5).containsOnly("Bearer token-1");
    }

    @Test
    void simultaneous_queries_without_a_token_share_a_single_signin() {
        SurrealTokenAuthentication filter = filter(signinReturning(Duration.ofHours(1)).delayElement(Duration.ofMillis(50)));

        StepVerifier.create(Flux.range(0, 20).flatMap(i -> filter.filter(query(), recording(HttpStatus.OK))))
                .expectNextCount(20)
                .verifyComplete();

        assertThat(signins).hasValue(1);
    }

    @Test
    void the_token_is_renewed_shortly_before_it_expires() {
        SurrealTokenAuthentication filter = filter(signinReturning(Duration.ofHours(1)));
        StepVerifier.create(filter.filter(query(), recording(HttpStatus.OK))).expectNextCount(1).verifyComplete();

        clock.set(NOW.plus(Duration.ofMinutes(58)));
        StepVerifier.create(filter.filter(query(), recording(HttpStatus.OK))).expectNextCount(1).verifyComplete();
        clock.set(NOW.plus(Duration.ofMinutes(59)).plusSeconds(30));
        StepVerifier.create(filter.filter(query(), recording(HttpStatus.OK))).expectNextCount(1).verifyComplete();

        assertThat(signins).hasValue(2);
        assertThat(authorizationHeaders).containsExactly("Bearer token-1", "Bearer token-1", "Bearer token-2");
    }

    @Test
    void a_rejected_token_triggers_one_new_signin_and_one_retry() {
        SurrealTokenAuthentication filter = filter(signinReturning(Duration.ofHours(1)));
        AtomicInteger calls = new AtomicInteger();
        ExchangeFunction unauthorizedOnce = request -> {
            record(request);
            return Mono.just(ClientResponse.create(calls.getAndIncrement() == 0 ? HttpStatus.UNAUTHORIZED : HttpStatus.OK)
                    .build());
        };

        StepVerifier.create(filter.filter(query(), unauthorizedOnce))
                .assertNext(response -> assertThat(response.statusCode().value()).isEqualTo(200))
                .verifyComplete();

        assertThat(signins).hasValue(2);
        assertThat(authorizationHeaders).containsExactly("Bearer token-1", "Bearer token-2");
    }

    @Test
    void a_second_rejection_is_returned_as_is_without_looping() {
        SurrealTokenAuthentication filter = filter(signinReturning(Duration.ofHours(1)));

        StepVerifier.create(filter.filter(query(), recording(HttpStatus.UNAUTHORIZED)))
                .assertNext(response -> assertThat(response.statusCode().value()).isEqualTo(401))
                .verifyComplete();

        assertThat(signins).hasValue(2);
        assertThat(authorizationHeaders).hasSize(2);
    }

    @Test
    void rejected_credentials_are_a_surreal_db_exception_and_no_query_is_sent() {
        WebClient signin = WebClient.builder().baseUrl("http://surreal.test")
                .exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.UNAUTHORIZED).build()))
                .build();
        SurrealTokenAuthentication filter = new SurrealTokenAuthentication(signin, PROPERTIES, new ObjectMapper(), clock::get);

        StepVerifier.create(filter.filter(query(), recording(HttpStatus.OK)))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(SurrealDbException.class)
                        .hasMessageContaining("401"))
                .verify();

        assertThat(authorizationHeaders).isEmpty();
    }

    @Test
    void a_token_without_exp_is_still_renewed_after_a_bounded_lifetime() {
        SurrealTokenAuthentication filter = filter(Mono.fromCallable(() -> "{\"token\":\"opaque-" + signins.incrementAndGet() + "\"}"));
        StepVerifier.create(filter.filter(query(), recording(HttpStatus.OK))).expectNextCount(1).verifyComplete();

        clock.set(NOW.plus(Duration.ofMinutes(10)));
        StepVerifier.create(filter.filter(query(), recording(HttpStatus.OK))).expectNextCount(1).verifyComplete();

        assertThat(authorizationHeaders).containsExactly("Bearer opaque-1", "Bearer opaque-2");
    }

    private SurrealTokenAuthentication filter(Mono<String> signinBody) {
        WebClient signin = WebClient.builder().baseUrl("http://surreal.test")
                .exchangeFunction(request -> signinBody.map(body -> ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", "application/json").body(body).build()))
                .build();
        return new SurrealTokenAuthentication(signin, PROPERTIES, new ObjectMapper(), clock::get);
    }

    /** Un cuerpo de {@code /signin} con un JWT {@code token-N.<payload>.firma} cuyo {@code exp} vence {@code lifetime} después del reloj. */
    private Mono<String> signinReturning(Duration lifetime) {
        return Mono.fromCallable(() -> {
            int number = signins.incrementAndGet();
            long exp = clock.get().plus(lifetime).getEpochSecond();
            String payload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(("{\"exp\":" + exp + "}").getBytes(StandardCharsets.UTF_8));
            return "{\"code\":200,\"token\":\"token-" + number + "." + payload + ".firma\"}";
        });
    }

    private ExchangeFunction recording(HttpStatus status) {
        return request -> {
            record(request);
            return Mono.just(ClientResponse.create(status).build());
        };
    }

    /** Guarda solo {@code Bearer token-N}: el resto del JWT no aporta a las aserciones. */
    private void record(ClientRequest request) {
        String authorization = request.headers().getFirst(HttpHeaders.AUTHORIZATION);
        authorizationHeaders.add(authorization == null ? "" : authorization.split("\\.")[0]);
    }

    private static ClientRequest query() {
        return ClientRequest.create(HttpMethod.POST, URI.create("http://surreal.test/sql")).build();
    }
}
