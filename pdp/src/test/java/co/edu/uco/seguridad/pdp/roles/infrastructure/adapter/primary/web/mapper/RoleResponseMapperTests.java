package co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.response.RoleWebResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RoleResponseMapperTests {

    private static final RoleId ROLE_ID = new RoleId(UUID.randomUUID());
    private static final RoleName NAME = new RoleName("Docente");
    private static final Instant REGISTERED_AT = Instant.parse("2026-09-11T00:00:00Z");

    @Test
    void flattens_an_application_scoped_role_with_both_ids() {
        TenantId tenant = new TenantId("universidad-uco");
        ApplicationId application = new ApplicationId(UUID.randomUUID());
        RoleResponse response = new RoleResponse(ROLE_ID, NAME, RoleScope.ofApplication(tenant, application),
                Set.of(), REGISTERED_AT);

        RoleWebResponse web = RoleResponseMapper.toResponse(response);

        assertThat(web.id()).isEqualTo(ROLE_ID.value().toString());
        assertThat(web.scope()).isEqualTo("APPLICATION");
        assertThat(web.tenantId()).isEqualTo(tenant.value());
        assertThat(web.applicationId()).isEqualTo(application.value().toString());
    }

    @Test
    void a_global_role_has_no_tenant_or_application_id() {
        RoleResponse response = new RoleResponse(ROLE_ID, NAME, RoleScope.global(), Set.of(), REGISTERED_AT);

        RoleWebResponse web = RoleResponseMapper.toResponse(response);

        assertThat(web.scope()).isEqualTo("GLOBAL");
        assertThat(web.tenantId()).isNull();
        assertThat(web.applicationId()).isNull();
    }

    @Test
    void carries_every_resource_id_as_a_string() {
        ResourceId resource = new ResourceId(UUID.randomUUID());
        RoleResponse response = new RoleResponse(ROLE_ID, NAME, RoleScope.global(), Set.of(resource), REGISTERED_AT);

        RoleWebResponse web = RoleResponseMapper.toResponse(response);

        assertThat(web.resourceIds()).containsExactly(resource.value().toString());
    }
}
