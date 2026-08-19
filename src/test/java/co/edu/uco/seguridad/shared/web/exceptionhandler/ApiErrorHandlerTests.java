package co.edu.uco.seguridad.shared.web.exceptionhandler;

import co.edu.uco.seguridad.pdp.applications.application.exception.DuplicateApplicationException;
import co.edu.uco.seguridad.pdp.applications.application.exception.ReservedApplicationNameException;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.commons.exception.InvalidApplicationNameException;
import co.edu.uco.seguridad.shared.web.exception.MissingRequestFieldException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El único {@code @RestControllerAdvice} del proyecto, probado directo (sin levantar el contexto
 * HTTP completo) para llegar a las ramas que un flujo de negocio real no toca: el 500 genérico y el
 * contrato sin correlación adjunta. Las rutas de negocio (400/409) ya están cubiertas de punta a
 * punta por {@code ProtectedApplicationHttpTests}; acá se cubre el manejador en sí mismo.
 */
class ApiErrorHandlerTests {

    private final ApiErrorHandler handler = new ApiErrorHandler();

    private static MockServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/protected-applications"));
    }

    @Test
    void a_request_contract_violation_becomes_a_400_with_the_offending_field() {
        ResponseEntity<ProblemDetail> response = handler.requestContract(
                new MissingRequestFieldException("applicationName"), exchange());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getProperties()).containsEntry("field", "applicationName");
    }

    @Test
    void an_invalid_value_becomes_a_400() {
        ResponseEntity<ProblemDetail> response = handler.invalidValue(
                new InvalidApplicationNameException("too short"), exchange());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getProperties()).containsEntry("code", "INVALID_APPLICATION_NAME");
    }

    @Test
    void a_conflicting_business_rule_becomes_a_409() {
        ResponseEntity<ProblemDetail> response = handler.businessRule(
                new DuplicateApplicationException(new TenantId("universidad-uco"),
                        new ApplicationName("gestion-academica")),
                exchange());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void a_non_conflicting_business_rule_becomes_a_400() {
        ResponseEntity<ProblemDetail> response = handler.businessRule(
                new ReservedApplicationNameException(new ApplicationName("admin")), exchange());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void an_unreadable_body_becomes_a_400() {
        ResponseEntity<ProblemDetail> response = handler.unreadableRequest(
                new ServerWebInputException("not valid JSON"), exchange());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getProperties()).containsEntry("code", "MALFORMED_REQUEST");
    }

    @Test
    void a_truly_unexpected_exception_becomes_a_generic_500_without_leaking_its_message() {
        ResponseEntity<ProblemDetail> response = handler.unexpected(
                new IllegalStateException("connection pool exhausted, host 10.0.0.7 unreachable"), exchange());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getDetail()).doesNotContain("10.0.0.7");
    }

    @Test
    void omits_correlation_properties_when_no_filter_ran_before_the_handler() {
        ResponseEntity<ProblemDetail> response = handler.invalidValue(
                new InvalidApplicationNameException("too short"), exchange());

        assertThat(response.getBody().getProperties()).doesNotContainKeys("requestId", "correlationId");
    }
}
