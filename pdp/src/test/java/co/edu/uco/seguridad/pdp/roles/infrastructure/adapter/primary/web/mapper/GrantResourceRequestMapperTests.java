package co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.GrantResourceRequest;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.request.raw.GrantResourceRawRequest;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import co.edu.uco.seguridad.shared.web.exception.MissingRequestFieldException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GrantResourceRequestMapperTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final String ROLE_ID = UUID.randomUUID().toString();
    private static final String RESOURCE_ID = UUID.randomUUID().toString();

    @Test
    void requires_the_resource_id_field() {
        assertThatThrownBy(() -> map(new GrantResourceRawRequest(ROLE_ID, null)))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(e -> ((MissingRequestFieldException) e).field())
                .isEqualTo("resourceId");
    }

    @Test
    void rejects_a_resource_id_that_is_not_a_valid_identifier() {
        assertThatThrownBy(() -> map(new GrantResourceRawRequest(ROLE_ID, "not-a-uuid")))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("resourceId");
    }

    @Test
    void rejects_a_role_id_that_is_not_a_valid_identifier() {
        assertThatThrownBy(() -> map(new GrantResourceRawRequest("not-a-uuid", RESOURCE_ID)))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("roleId");
    }

    @Test
    void builds_the_request_with_the_tenant_from_the_principal() {
        GrantResourceRequest request = map(new GrantResourceRawRequest(ROLE_ID, RESOURCE_ID));

        assertThat(request.tenantId()).isEqualTo(TENANT);
        assertThat(request.roleId()).isEqualTo(RoleId.of(ROLE_ID));
        assertThat(request.resourceId()).isEqualTo(ResourceId.of(RESOURCE_ID));
    }

    private static GrantResourceRequest map(GrantResourceRawRequest raw) {
        return GrantResourceRequestMapper.toRequest(raw, TENANT);
    }
}
