package co.edu.uco.seguridad.pdp.roles.domain.model;

import co.edu.uco.seguridad.pdp.roles.domain.exception.InvalidRoleScopeException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleScopeLevelTests {

    @Test
    void parses_case_insensitively() {
        assertThat(RoleScopeLevel.parse("global")).isEqualTo(RoleScopeLevel.GLOBAL);
        assertThat(RoleScopeLevel.parse("Tenant")).isEqualTo(RoleScopeLevel.TENANT);
        assertThat(RoleScopeLevel.parse("APPLICATION")).isEqualTo(RoleScopeLevel.APPLICATION);
    }

    @Test
    void rejects_a_blank_value() {
        assertThatThrownBy(() -> RoleScopeLevel.parse(" ")).isInstanceOf(InvalidRoleScopeException.class);
    }

    @Test
    void rejects_a_level_the_platform_does_not_support() {
        assertThatThrownBy(() -> RoleScopeLevel.parse("ORGANIZATION")).isInstanceOf(InvalidRoleScopeException.class);
    }

    @Test
    void only_tenant_and_application_require_a_tenant() {
        assertThat(RoleScopeLevel.GLOBAL.requiresTenant()).isFalse();
        assertThat(RoleScopeLevel.TENANT.requiresTenant()).isTrue();
        assertThat(RoleScopeLevel.APPLICATION.requiresTenant()).isTrue();
    }

    @Test
    void only_application_requires_an_application() {
        assertThat(RoleScopeLevel.GLOBAL.requiresApplication()).isFalse();
        assertThat(RoleScopeLevel.TENANT.requiresApplication()).isFalse();
        assertThat(RoleScopeLevel.APPLICATION.requiresApplication()).isTrue();
    }
}
