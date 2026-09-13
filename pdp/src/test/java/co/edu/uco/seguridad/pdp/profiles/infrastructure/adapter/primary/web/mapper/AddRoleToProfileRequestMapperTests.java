package co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.AddRoleToProfileRequest;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.request.raw.AddRoleToProfileRawRequest;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import co.edu.uco.seguridad.shared.web.exception.MissingRequestFieldException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Espejo de GrantResourceRequestMapperTests. */
class AddRoleToProfileRequestMapperTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final String PROFILE_ID = UUID.randomUUID().toString();
    private static final String ROLE_ID = UUID.randomUUID().toString();

    @Test
    void requires_the_role_id_field() {
        assertThatThrownBy(() -> map(new AddRoleToProfileRawRequest(PROFILE_ID, null)))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(e -> ((MissingRequestFieldException) e).field())
                .isEqualTo("roleId");
    }

    @Test
    void rejects_a_role_id_that_is_not_a_valid_identifier() {
        assertThatThrownBy(() -> map(new AddRoleToProfileRawRequest(PROFILE_ID, "not-a-uuid")))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("roleId");
    }

    @Test
    void rejects_a_profile_id_that_is_not_a_valid_identifier() {
        assertThatThrownBy(() -> map(new AddRoleToProfileRawRequest("not-a-uuid", ROLE_ID)))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("profileId");
    }

    @Test
    void builds_the_request_with_the_tenant_from_the_principal() {
        AddRoleToProfileRequest request = map(new AddRoleToProfileRawRequest(PROFILE_ID, ROLE_ID));

        assertThat(request.tenantId()).isEqualTo(TENANT);
        assertThat(request.profileId()).isEqualTo(ProfileId.of(PROFILE_ID));
        assertThat(request.roleId()).isEqualTo(RoleId.of(ROLE_ID));
    }

    private static AddRoleToProfileRequest map(AddRoleToProfileRawRequest raw) {
        return AddRoleToProfileRequestMapper.toRequest(raw, TENANT);
    }
}
