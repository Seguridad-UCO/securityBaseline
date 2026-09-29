package co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse;
import co.edu.uco.seguridad.pdp.profiles.domain.model.ProfileName;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.response.ProfileWebResponse;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Espejo de RoleResponseMapperTests.
 */
class ProfileResponseMapperTests {

    private static final ProfileId PROFILE_ID = new ProfileId(UUID.randomUUID());
    private static final Instant REGISTERED_AT = Instant.parse("2026-09-12T00:00:00Z");

    @Test
    void flattens_an_application_scoped_profile_with_both_ids() {
        TenantId tenant = new TenantId("universidad-uco");
        ApplicationId application = new ApplicationId(UUID.randomUUID());
        ProfileResponse response = new ProfileResponse(PROFILE_ID, new ProfileName("Coordinador académico"),
                RoleScope.ofApplication(tenant, application), Set.of(), REGISTERED_AT);

        ProfileWebResponse web = ProfileResponseMapper.toResponse(response);

        assertThat(web.id()).isEqualTo(PROFILE_ID.value().toString());
        assertThat(web.scope()).isEqualTo("APPLICATION");
        assertThat(web.tenantId()).isEqualTo(tenant.value());
        assertThat(web.applicationId()).isEqualTo(application.value().toString());
    }

    @Test
    void a_global_profile_has_no_tenant_or_application_id() {
        ProfileResponse response = new ProfileResponse(PROFILE_ID, new ProfileName("Coordinador académico"),
                RoleScope.global(), Set.of(), REGISTERED_AT);

        ProfileWebResponse web = ProfileResponseMapper.toResponse(response);

        assertThat(web.scope()).isEqualTo("GLOBAL");
        assertThat(web.tenantId()).isNull();
        assertThat(web.applicationId()).isNull();
    }

    @Test
    void carries_every_role_id_as_a_string() {
        RoleId role = new RoleId(UUID.randomUUID());
        ProfileResponse response = new ProfileResponse(PROFILE_ID, new ProfileName("Coordinador académico"),
                RoleScope.global(), Set.of(role), REGISTERED_AT);

        ProfileWebResponse web = ProfileResponseMapper.toResponse(response);

        assertThat(web.roleIds()).containsExactly(role.value().toString());
    }
}
