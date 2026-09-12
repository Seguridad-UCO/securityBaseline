package co.edu.uco.seguridad.pdp.roles.domain;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RoleCriteriaTests {

    private static final TenantId UCO = new TenantId("universidad-uco");
    private static final TenantId OTRO = new TenantId("otra-universidad");
    private static final Instant REGISTERED_AT = Instant.parse("2026-09-11T00:00:00Z");

    @Test
    void matches_a_role_of_the_same_tenant() {
        RoleCriteria criteria = RoleCriteria.ofTenant(UCO);

        assertThat(criteria.matches(role(RoleScope.ofTenant(UCO)))).isTrue();
    }

    @Test
    void matches_a_global_role_regardless_of_tenant() {
        RoleCriteria criteria = RoleCriteria.ofTenant(UCO);

        assertThat(criteria.matches(role(RoleScope.global()))).isTrue();
    }

    @Test
    void rejects_a_role_of_another_tenant() {
        RoleCriteria criteria = RoleCriteria.ofTenant(UCO);

        assertThat(criteria.matches(role(RoleScope.ofTenant(OTRO)))).isFalse();
    }

    @Test
    void matches_an_application_role_whose_tenant_is_the_same() {
        RoleCriteria criteria = RoleCriteria.ofTenant(UCO);
        RoleScope scope = RoleScope.ofApplication(UCO, new ApplicationId(UUID.randomUUID()));

        assertThat(criteria.matches(role(scope))).isTrue();
    }

    private static Role role(RoleScope scope) {
        return Role.define(new RoleId(UUID.randomUUID()), new RoleName("Docente"), scope, REGISTERED_AT);
    }
}
