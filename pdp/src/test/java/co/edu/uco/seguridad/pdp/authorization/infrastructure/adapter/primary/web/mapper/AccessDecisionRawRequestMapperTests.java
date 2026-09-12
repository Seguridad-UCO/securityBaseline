package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AccessDecisionRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AccessDecisionRawRequest.RawApplication;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AccessDecisionRawRequest.RawContext;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AccessDecisionRawRequest.RawResource;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import co.edu.uco.seguridad.shared.web.exception.MissingRequestFieldException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Barreras C1/C4 de la sección 3 del plan (versión y timestamp) — C2/C3 (coherencia con los headers)
 * las prueba {@code InternalAccessDecisionInteractorImplTests}, que es quien tiene el
 * {@code RequestContext}.
 */
class AccessDecisionRawRequestMapperTests {

    private static final String APPLICATION_ID = UUID.randomUUID().toString();
    private static final String SUBJECT = "evidence-subject";

    private static AccessDecisionRawRequest rawWith(String version, String applicationId, String path,
            String action, String timestamp) {
        return new AccessDecisionRawRequest(version, "req-1", "corr-1", timestamp,
                new RawApplication(applicationId, "prod"), new RawResource(path, action),
                new RawContext(action, "HTTP"));
    }

    @Test
    void requires_the_version_field() {
        assertThatThrownBy(() -> AccessDecisionRawRequestMapper.toRequest(
                rawWith(null, APPLICATION_ID, "/estudiantes", "GET", "2026-09-11T00:00:00Z"), SUBJECT))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(e -> ((MissingRequestFieldException) e).field())
                .isEqualTo("version");
    }

    @Test
    void rejects_a_version_different_from_1() {
        assertThatThrownBy(() -> AccessDecisionRawRequestMapper.toRequest(
                rawWith("2", APPLICATION_ID, "/estudiantes", "GET", "2026-09-11T00:00:00Z"), SUBJECT))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("version");
    }

    @Test
    void requires_the_application_id_field() {
        assertThatThrownBy(() -> AccessDecisionRawRequestMapper.toRequest(
                rawWith("1", null, "/estudiantes", "GET", "2026-09-11T00:00:00Z"), SUBJECT))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(e -> ((MissingRequestFieldException) e).field())
                .isEqualTo("applicationId");
    }

    @Test
    void rejects_an_application_id_that_is_not_a_valid_identifier() {
        assertThatThrownBy(() -> AccessDecisionRawRequestMapper.toRequest(
                rawWith("1", "not-a-uuid", "/estudiantes", "GET", "2026-09-11T00:00:00Z"), SUBJECT))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("applicationId");
    }

    @Test
    void requires_the_resource_path_field() {
        assertThatThrownBy(() -> AccessDecisionRawRequestMapper.toRequest(
                rawWith("1", APPLICATION_ID, null, "GET", "2026-09-11T00:00:00Z"), SUBJECT))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(e -> ((MissingRequestFieldException) e).field())
                .isEqualTo("resourcePath");
    }

    @Test
    void rejects_an_unsupported_action() {
        assertThatThrownBy(() -> AccessDecisionRawRequestMapper.toRequest(
                rawWith("1", APPLICATION_ID, "/estudiantes", "TRACE", "2026-09-11T00:00:00Z"), SUBJECT))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("action");
    }

    @Test
    void requires_the_timestamp_field() {
        assertThatThrownBy(() -> AccessDecisionRawRequestMapper.toRequest(
                rawWith("1", APPLICATION_ID, "/estudiantes", "GET", null), SUBJECT))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(e -> ((MissingRequestFieldException) e).field())
                .isEqualTo("timestamp");
    }

    @Test
    void rejects_a_timestamp_that_is_not_iso_8601() {
        assertThatThrownBy(() -> AccessDecisionRawRequestMapper.toRequest(
                rawWith("1", APPLICATION_ID, "/estudiantes", "GET", "not-a-timestamp"), SUBJECT))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("timestamp");
    }

    @Test
    void builds_the_request_with_the_subject_from_the_evidence_jwt_not_the_body() {
        var request = AccessDecisionRawRequestMapper.toRequest(
                rawWith("1", APPLICATION_ID, "/estudiantes", "GET", "2026-09-11T00:00:00Z"), SUBJECT);

        assertThat(request.subject()).isEqualTo(SUBJECT);
        assertThat(request.applicationId().value()).isEqualTo(UUID.fromString(APPLICATION_ID));
        assertThat(request.resourcePath().value()).isEqualTo("/estudiantes");
        assertThat(request.action()).isEqualTo(HttpVerb.GET);
        assertThat(request.requestId()).isEqualTo("req-1");
        assertThat(request.correlationId()).isEqualTo("corr-1");
    }
}
