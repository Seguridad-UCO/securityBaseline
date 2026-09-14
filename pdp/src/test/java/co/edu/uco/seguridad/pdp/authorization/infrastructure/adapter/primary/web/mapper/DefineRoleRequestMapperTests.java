package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.DefineRoleRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.DefineRoleRequest;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import co.edu.uco.seguridad.shared.web.exception.MissingRequestFieldException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Barreras C1 (scope solo TENANT/APPLICATION por este canal) y C2 (applicationId exigido/prohibido
 * según el nivel) — HU-016: movido desde {@code roles}, mismos casos, ahora sobre el mapper que vive
 * en {@code authorization} (PLAN-HU-016.md §7).
 */
class DefineRoleRequestMapperTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final String APPLICATION = UUID.randomUUID().toString();

    @Test
    void requires_the_name_field() {
        assertThatThrownBy(() -> map(new DefineRoleRawRequest(null, "TENANT", null)))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(e -> ((MissingRequestFieldException) e).field())
                .isEqualTo("name");
    }

    @Test
    void requires_the_scope_field() {
        assertThatThrownBy(() -> map(new DefineRoleRawRequest("Docente", null, null)))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(e -> ((MissingRequestFieldException) e).field())
                .isEqualTo("scope");
    }

    @Test
    void rejects_a_global_scope_on_this_channel() {
        assertThatThrownBy(() -> map(new DefineRoleRawRequest("Docente", "GLOBAL", null)))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("scope");
    }

    @Test
    void rejects_a_scope_value_the_platform_does_not_support() {
        assertThatThrownBy(() -> map(new DefineRoleRawRequest("Docente", "ORGANIZATION", null)))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("scope");
    }

    @Test
    void application_scope_requires_an_application_id() {
        assertThatThrownBy(() -> map(new DefineRoleRawRequest("Docente", "APPLICATION", null)))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(e -> ((MissingRequestFieldException) e).field())
                .isEqualTo("applicationId");
    }

    @Test
    void application_scope_rejects_a_malformed_application_id() {
        assertThatThrownBy(() -> map(new DefineRoleRawRequest("Docente", "APPLICATION", "not-a-uuid")))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("applicationId");
    }

    @Test
    void tenant_scope_rejects_an_application_id() {
        assertThatThrownBy(() -> map(new DefineRoleRawRequest("Docente", "TENANT", APPLICATION)))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("applicationId");
    }

    @Test
    void builds_a_tenant_scoped_request_from_the_principal() {
        DefineRoleRequest request = map(new DefineRoleRawRequest("  Docente  ", "TENANT", null));

        assertThat(request.name().value()).isEqualTo("Docente");
        assertThat(request.scope()).isEqualTo(RoleScope.ofTenant(TENANT));
    }

    @Test
    void builds_an_application_scoped_request() {
        DefineRoleRequest request = map(new DefineRoleRawRequest("Docente", "application", APPLICATION));

        assertThat(request.scope()).isEqualTo(RoleScope.ofApplication(TENANT, ApplicationId.of(APPLICATION)));
    }

    private static DefineRoleRequest map(DefineRoleRawRequest raw) {
        return DefineRoleRequestMapper.toRequest(raw, TENANT);
    }
}
