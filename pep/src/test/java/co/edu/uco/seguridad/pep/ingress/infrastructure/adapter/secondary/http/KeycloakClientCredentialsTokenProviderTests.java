package co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.secondary.http;

import co.edu.uco.seguridad.pep.commons.EnforcementFailure;
import co.edu.uco.seguridad.pep.ingress.infrastructure.properties.PdpServiceIdentityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakClientCredentialsTokenProviderTests {

    @Test
    void obtains_and_caches_a_client_credentials_token_until_its_refresh_window() {
        AtomicInteger calls = new AtomicInteger();
        ExchangeFunction exchange = request -> {
            calls.incrementAndGet();
            assertThat(request.headers().getContentType().toString()).isEqualTo("application/x-www-form-urlencoded");
            return Mono.just(ClientResponse.create(HttpStatus.OK).header("Content-Type", "application/json")
                    .body("{\"access_token\":\"service-token\",\"expires_in\":300}").build());
        };
        KeycloakClientCredentialsTokenProvider provider = provider(properties(), exchange);

        StepVerifier.create(provider.token()).expectNext("service-token").verifyComplete();
        StepVerifier.create(provider.token()).expectNext("service-token").verifyComplete();

        assertThat(calls).hasValue(1);
    }

    @Test
    void missing_configuration_fails_closed_without_an_http_call() {
        KeycloakClientCredentialsTokenProvider provider = provider(
                new PdpServiceIdentityProperties(null, "", "", Duration.ofSeconds(1), Duration.ZERO),
                request -> Mono.error(new AssertionError("must not call Keycloak")));

        StepVerifier.create(provider.token()).expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(EnforcementFailure.class);
            assertThat(((EnforcementFailure) error).kind()).isEqualTo(EnforcementFailure.Kind.UNAVAILABLE);
            assertThat(((EnforcementFailure) error).code()).isEqualTo("PDP_SERVICE_IDENTITY_UNAVAILABLE");
        }).verify();
    }

    private static PdpServiceIdentityProperties properties() {
        return new PdpServiceIdentityProperties(URI.create("http://localhost:9090/token"), "pep", "secret",
                Duration.ofSeconds(1), Duration.ofSeconds(30));
    }

    private static KeycloakClientCredentialsTokenProvider provider(PdpServiceIdentityProperties properties,
                                                                   ExchangeFunction exchange) {
        return new KeycloakClientCredentialsTokenProvider(properties,
                WebClient.builder().exchangeFunction(exchange).build());
    }
}
