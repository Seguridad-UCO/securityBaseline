package co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.DefineProfileRequest;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.request.raw.DefineProfileRawRequest;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import co.edu.uco.seguridad.shared.web.exception.MissingRequestFieldException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Espejo de DefineRoleRequestMapperTests: mismas barreras C1/C2, scope GLOBAL bloqueado hasta HU-009. */
class DefineProfileRequestMapperTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final String APPLICATION = UUID.randomUUID().toString();

    @Test
    void requires_the_name_field() {
        assertThatThrownBy(() -> map(new DefineProfileRawRequest(null, "TENANT", null)))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(e -> ((MissingRequestFieldException) e).field())
                .isEqualTo("name");
    }

    @Test
    void requires_the_scope_field() {
        assertThatThrownBy(() -> map(new DefineProfileRawRequest("Coordinador académico", null, null)))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(e -> ((MissingRequestFieldException) e).field())
                .isEqualTo("scope");
    }

    @Test
    void rejects_a_global_scope_on_this_channel() {
        assertThatThrownBy(() -> map(new DefineProfileRawRequest("Coordinador académico", "GLOBAL", null)))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("scope");
    }

    @Test
    void rejects_a_scope_value_the_platform_does_not_support() {
        assertThatThrownBy(() -> map(new DefineProfileRawRequest("Coordinador académico", "ORGANIZATION", null)))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("scope");
    }

    @Test
    void application_scope_requires_an_application_id() {
        assertThatThrownBy(() -> map(new DefineProfileRawRequest("Coordinador académico", "APPLICATION", null)))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(e -> ((MissingRequestFieldException) e).field())
                .isEqualTo("applicationId");
    }

    @Test
    void application_scope_rejects_a_malformed_application_id() {
        assertThatThrownBy(() -> map(new DefineProfileRawRequest("Coordinador académico", "APPLICATION", "not-a-uuid")))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("applicationId");
    }

    @Test
    void tenant_scope_rejects_an_application_id() {
        assertThatThrownBy(() -> map(new DefineProfileRawRequest("Coordinador académico", "TENANT", APPLICATION)))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("applicationId");
    }

    @Test
    void builds_a_tenant_scoped_request_from_the_principal() {
        DefineProfileRequest request = map(new DefineProfileRawRequest("  Coordinador académico  ", "TENANT", null));

        assertThat(request.name().value()).isEqualTo("Coordinador académico");
        assertThat(request.scope()).isEqualTo(RoleScope.ofTenant(TENANT));
    }

    @Test
    void builds_an_application_scoped_request() {
        DefineProfileRequest request = map(new DefineProfileRawRequest("Coordinador académico", "application", APPLICATION));

        assertThat(request.scope()).isEqualTo(RoleScope.ofApplication(TENANT, ApplicationId.of(APPLICATION)));
    }

    private static DefineProfileRequest map(DefineProfileRawRequest raw) {
        return DefineProfileRequestMapper.toRequest(raw, TENANT);
    }
}
