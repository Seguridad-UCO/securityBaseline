package co.edu.uco.seguridad.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * D2: sin certificado de cliente, o con un sujeto fuera de la lista admitida, la cadena nunca se
 * invoca — falla cerrado. El esqueleto lanza incondicionalmente; cada caso documenta el escenario
 * que el implementador tiene que discriminar.
 */
class InternalMtlsWebFilterTests {

    private static final WebFilterChain NEVER_CALLED = exchange -> {
        throw new AssertionError("must not call the chain: mTLS must reject first");
    };

    @Test
    void rejects_a_request_without_a_client_certificate() {
        InternalMtlsWebFilter filter = new InternalMtlsWebFilter(new InternalMtlsProperties("ca.pem", List.of("CN=pep")));
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.post("/internal/v1/access-decisions").build());

        assertThatThrownBy(() -> filter.filter(exchange, NEVER_CALLED))
                .isInstanceOf(UnsupportedOperationException.class);
        // Contrato final esperado (D2): responde 403 sin invocar la cadena.
    }

    @Test
    void rejects_a_client_certificate_whose_subject_is_not_admitted() {
        InternalMtlsWebFilter filter = new InternalMtlsWebFilter(new InternalMtlsProperties("ca.pem", List.of("CN=pep")));
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.post("/internal/v1/access-decisions").build());

        assertThatThrownBy(() -> filter.filter(exchange, NEVER_CALLED))
                .isInstanceOf(UnsupportedOperationException.class);
        // Contrato final esperado: SslInfo con un certificado cuyo Subject DN no está en
        // allowedSubjects() -> 403 sin invocar la cadena.
    }

    @Test
    void continues_the_chain_when_the_certificate_subject_is_admitted() {
        InternalMtlsWebFilter filter = new InternalMtlsWebFilter(new InternalMtlsProperties("ca.pem", List.of("CN=pep")));
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.post("/internal/v1/access-decisions").build());

        assertThatThrownBy(() -> filter.filter(exchange, ex -> Mono.empty()))
                .isInstanceOf(UnsupportedOperationException.class);
        // Contrato final esperado: SslInfo con un certificado admitido -> chain.filter(exchange).
    }
}
