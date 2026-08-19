package co.edu.uco.seguridad.shared.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code valueOrRandom} tiene tres entradas, no dos: cabecera ausente, cabecera presente, y
 * cabecera presente pero en blanco (un cliente que manda {@code X-Request-Id: } vacío no debería
 * heredar esa cadena vacía como id). Las pruebas HTTP de extremo a extremo solo ejercitan las
 * primeras dos.
 */
class CorrelationWebFilterTests {

    private final CorrelationWebFilter filter = new CorrelationWebFilter();

    @Test
    void keeps_the_client_supplied_ids_when_present() {
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/x")
                .header("X-Request-Id", "req-42")
                .header("X-Correlation-Id", "corr-7"));

        filter.filter(exchange, chain).block();

        RequestContext context = CorrelationWebFilter.context(exchange);
        assertThat(context.requestId()).isEqualTo("req-42");
        assertThat(context.correlationId()).isEqualTo("corr-7");
    }

    @Test
    void generates_a_random_id_when_the_header_is_absent() {
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/x"));

        filter.filter(exchange, chain).block();

        RequestContext context = CorrelationWebFilter.context(exchange);
        assertThat(context.requestId()).isNotBlank();
        assertThat(context.correlationId()).isNotBlank();
    }

    @Test
    void generates_a_random_id_when_the_header_is_present_but_blank() {
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/x")
                .header("X-Request-Id", "   "));

        filter.filter(exchange, chain).block();

        assertThat(CorrelationWebFilter.context(exchange).requestId()).isNotBlank();
    }

    @Test
    void has_no_context_before_the_filter_runs() {
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/x"));

        assertThat(CorrelationWebFilter.context(exchange)).isNull();
    }

    private static final org.springframework.web.server.WebFilterChain chain =
            exchange -> Mono.empty();
}
