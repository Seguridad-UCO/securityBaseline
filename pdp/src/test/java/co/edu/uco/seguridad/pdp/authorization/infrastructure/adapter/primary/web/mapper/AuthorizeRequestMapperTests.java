package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AuthorizeRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import co.edu.uco.seguridad.shared.web.exception.MissingRequestFieldException;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizeRequestMapperTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final String APPLICATION_ID = UUID.randomUUID().toString();
    private static final UserId USER_ID = new UserId(UUID.randomUUID());

    @Test
    void requires_the_application_id_field() {
        assertThatThrownBy(() -> AuthorizeRequestMapper.toRequest(
                new AuthorizeRawRequest(null, "/estudiantes", "GET"), TENANT, "test-subject", "req-1", "corr-1",
                Optional.empty()))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(e -> ((MissingRequestFieldException) e).field())
                .isEqualTo("applicationId");
    }

    @Test
    void requires_the_resource_path_field() {
        assertThatThrownBy(() -> AuthorizeRequestMapper.toRequest(
                new AuthorizeRawRequest(APPLICATION_ID, null, "GET"), TENANT, "test-subject", "req-1", "corr-1",
                Optional.empty()))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(e -> ((MissingRequestFieldException) e).field())
                .isEqualTo("resourcePath");
    }

    @Test
    void requires_the_action_field() {
        assertThatThrownBy(() -> AuthorizeRequestMapper.toRequest(
                new AuthorizeRawRequest(APPLICATION_ID, "/estudiantes", null), TENANT, "test-subject", "req-1", "corr-1",
                Optional.empty()))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(e -> ((MissingRequestFieldException) e).field())
                .isEqualTo("action");
    }

    @Test
    void rejects_an_application_id_that_is_not_a_valid_identifier() {
        assertThatThrownBy(() -> AuthorizeRequestMapper.toRequest(
                new AuthorizeRawRequest("not-a-uuid", "/estudiantes", "GET"), TENANT, "test-subject", "req-1", "corr-1",
                Optional.empty()))
                .isInstanceOf(MalformedRequestFieldException.class);
    }

    @Test
    void rejects_an_unsupported_action() {
        assertThatThrownBy(() -> AuthorizeRequestMapper.toRequest(
                new AuthorizeRawRequest(APPLICATION_ID, "/estudiantes", "TRACE"), TENANT, "test-subject", "req-1", "corr-1",
                Optional.empty()))
                .isInstanceOf(MalformedRequestFieldException.class);
    }

    @Test
    void builds_the_request_with_the_tenant_and_subject_from_the_principal_not_the_body() {
        var request = AuthorizeRequestMapper.toRequest(
                new AuthorizeRawRequest(APPLICATION_ID, "/estudiantes", "GET"), TENANT, "test-subject", "req-1", "corr-1",
                Optional.empty());

        assertThat(request.tenantId()).isEqualTo(TENANT);
        assertThat(request.subject()).isEqualTo("test-subject");
        assertThat(request.correlationId()).isEqualTo("corr-1");
        assertThat(request.subjectRoles()).isEmpty();
    }

    @Test
    void propagates_the_resolved_subject_user_id_as_is() {
        var request = AuthorizeRequestMapper.toRequest(
                new AuthorizeRawRequest(APPLICATION_ID, "/estudiantes", "GET"), TENANT, "test-subject", "req-1", "corr-1",
                Optional.of(USER_ID));

        assertThat(request.subjectUserId()).contains(USER_ID);
    }
}
