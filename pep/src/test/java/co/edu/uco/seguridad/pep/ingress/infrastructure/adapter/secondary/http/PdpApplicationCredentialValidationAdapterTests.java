package co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.secondary.http;

import co.edu.uco.seguridad.pep.commons.EnforcementFailure;
import co.edu.uco.seguridad.pep.ingress.application.port.secondary.PdpServiceTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


import static org.assertj.core.api.Assertions.assertThat;

class PdpApplicationCredentialValidationAdapterTests {
    private final PdpServiceTokenProvider token = () -> Mono.just("pep-evidence-jwt");

    @Test
    void accepts_a_credential_only_when_the_pdp_confirms_it() {
        ExchangeFunction exchange = request -> {
            assertThat(request.url().getPath()).isEqualTo("/internal/v1/applications/app-1/credential-validations");
            assertThat(request.headers().getFirst("Authorization")).isEqualTo("Bearer pep-evidence-jwt");
            return Mono.just(ClientResponse.create(HttpStatus.OK).header("Content-Type", "application/json").body("{}").build());
        };

        StepVerifier.create(adapter(exchange).validate("app-1", "application-secret"))
                .verifyComplete();
    }

    @Test
    void turns_an_invalid_application_credential_into_unauthenticated() {
        StepVerifier.create(adapter(request -> Mono.just(ClientResponse.create(HttpStatus.BAD_REQUEST).build()))
                        .validate("app-1", "wrong-secret"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(EnforcementFailure.class);
                    assertThat(((EnforcementFailure) error).kind()).isEqualTo(EnforcementFailure.Kind.UNAUTHENTICATED);
                    assertThat(((EnforcementFailure) error).code()).isEqualTo("INTEGRATION_TOKEN_INVALID");
                }).verify();
    }

    @Test
    void fails_closed_when_the_pdp_channel_is_unavailable() {
        StepVerifier.create(adapter(request -> Mono.error(new IllegalStateException("network down")))
                        .validate("app-1", "application-secret"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(EnforcementFailure.class);
                    assertThat(((EnforcementFailure) error).kind()).isEqualTo(EnforcementFailure.Kind.UNAVAILABLE);
                }).verify();
    }

    private PdpApplicationCredentialValidationAdapter adapter(ExchangeFunction exchange) {
        return new PdpApplicationCredentialValidationAdapter(WebClient.builder().exchangeFunction(exchange).build(), token);
    }
}
