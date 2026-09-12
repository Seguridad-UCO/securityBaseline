package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignRoleRequest;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.AssignRoleRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import co.edu.uco.seguridad.shared.web.exception.MissingRequestFieldException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssignRoleRequestMapperTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final String ROLE_ID = UUID.randomUUID().toString();
    private static final String USER_ID = UUID.randomUUID().toString();
    private static final String APPLICATION_ID = UUID.randomUUID().toString();

    @Test
    void requires_the_user_id_field() {
        assertThatThrownBy(() -> map(new AssignRoleRawRequest(ROLE_ID, null, APPLICATION_ID)))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(e -> ((MissingRequestFieldException) e).field())
                .isEqualTo("userId");
    }

    @Test
    void rejects_a_user_id_that_is_not_a_valid_identifier() {
        assertThatThrownBy(() -> map(new AssignRoleRawRequest(ROLE_ID, "not-a-uuid", APPLICATION_ID)))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("userId");
    }

    @Test
    void requires_the_application_id_field() {
        assertThatThrownBy(() -> map(new AssignRoleRawRequest(ROLE_ID, USER_ID, null)))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(e -> ((MissingRequestFieldException) e).field())
                .isEqualTo("applicationId");
    }

    @Test
    void rejects_an_application_id_that_is_not_a_valid_identifier() {
        assertThatThrownBy(() -> map(new AssignRoleRawRequest(ROLE_ID, USER_ID, "not-a-uuid")))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("applicationId");
    }

    @Test
    void builds_the_request_with_the_tenant_from_the_principal() {
        AssignRoleRequest request = map(new AssignRoleRawRequest(ROLE_ID, USER_ID, APPLICATION_ID));

        assertThat(request.tenantId()).isEqualTo(TENANT);
        assertThat(request.roleId()).isEqualTo(RoleId.of(ROLE_ID));
        assertThat(request.userId()).isEqualTo(UserId.of(USER_ID));
        assertThat(request.applicationId()).isEqualTo(ApplicationId.of(APPLICATION_ID));
    }

    private static AssignRoleRequest map(AssignRoleRawRequest raw) {
        return AssignRoleRequestMapper.toRequest(raw, TENANT);
    }
}
