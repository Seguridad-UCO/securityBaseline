package co.edu.uco.seguridad.shared.persistence.surrealdb;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code execute}'s error branches — SurrealDB rejects the whole request, or rejects one statement
 * inside a batch — never happen against the real container in {@code SurrealRepositoryIntegrationTests}
 * (every SurrealQL there is deliberately well-formed). A stub {@link WebClient} exercises them
 * without a live server, which is also the only way {@link SurrealDbException} itself gets
 * constructed by anything other than test code.
 */
class SurrealDbClientTests {

    private static SurrealDbClient clientReturning(String body) {
        WebClient webClient = WebClient.builder()
                .baseUrl("http://surreal.test")
                .exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body(body)
                        .build()))
                .build();
        SurrealDbProperties properties = new SurrealDbProperties(
                "http://surreal.test", "pdp", "pdp", "root", "root");
        return new SurrealDbClient(webClient, new ObjectMapper(), properties);
    }

    @Test
    void a_response_that_is_not_an_array_is_a_surreal_db_exception() {
        SurrealDbClient client = clientReturning("{\"information\":\"malformed request\"}");

        StepVerifier.create(client.execute("BOGUS;", Map.of()))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(SurrealDbException.class)
                        .hasMessage("malformed request"))
                .verify();
    }

    @Test
    void a_statement_with_a_non_ok_status_is_a_surreal_db_exception() {
        SurrealDbClient client = clientReturning(
                "[{\"status\":\"ERR\",\"result\":\"Database `pdp` already exists\"}]");

        StepVerifier.create(client.execute("DEFINE DATABASE pdp;", Map.of()))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(SurrealDbException.class)
                        .hasMessage("Database `pdp` already exists"))
                .verify();
    }

    @Test
    void every_statement_result_is_returned_in_order() {
        SurrealDbClient client = clientReturning(
                "[{\"status\":\"OK\",\"result\":1},{\"status\":\"OK\",\"result\":2}]");

        StepVerifier.create(client.execute("SELECT 1; SELECT 2;", Map.of()))
                .assertNext(results -> assertThat(results)
                        .extracting(node -> node.asInt())
                        .containsExactly(1, 2))
                .verifyComplete();
    }
}
