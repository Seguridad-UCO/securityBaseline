package co.edu.uco.seguridad.pdp.roles.domain.model;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.roles.domain.exception.InvalidRoleScopeException;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Invariante L2 del plan: cada nivel exige exactamente los identificadores que le corresponden,
 * ni más ni menos (INV-DAT-01).
 */
class RoleScopeTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());

    @Test
    void global_has_no_identifiers() {
        RoleScope scope = RoleScope.global();

        assertThat(scope.level()).isEqualTo(RoleScopeLevel.GLOBAL);
        assertThat(scope.tenantId()).isEmpty();
        assertThat(scope.applicationId()).isEmpty();
    }

    @Test
    void tenant_carries_only_the_tenant() {
        RoleScope scope = RoleScope.ofTenant(TENANT);

        assertThat(scope.level()).isEqualTo(RoleScopeLevel.TENANT);
        assertThat(scope.tenantId()).contains(TENANT);
        assertThat(scope.applicationId()).isEmpty();
    }

    @Test
    void application_carries_tenant_and_application() {
        RoleScope scope = RoleScope.ofApplication(TENANT, APPLICATION);

        assertThat(scope.level()).isEqualTo(RoleScopeLevel.APPLICATION);
        assertThat(scope.tenantId()).contains(TENANT);
        assertThat(scope.applicationId()).contains(APPLICATION);
    }

    @Test
    void global_rejects_a_tenant_id() {
        assertThatThrownBy(() -> new RoleScope(RoleScopeLevel.GLOBAL, Optional.of(TENANT), Optional.empty()))
                .isInstanceOf(InvalidRoleScopeException.class);
    }

    @Test
    void tenant_requires_a_tenant_id() {
        assertThatThrownBy(() -> new RoleScope(RoleScopeLevel.TENANT, Optional.empty(), Optional.empty()))
                .isInstanceOf(InvalidRoleScopeException.class);
    }

    @Test
    void tenant_rejects_an_application_id() {
        assertThatThrownBy(() -> new RoleScope(RoleScopeLevel.TENANT, Optional.of(TENANT), Optional.of(APPLICATION)))
                .isInstanceOf(InvalidRoleScopeException.class);
    }

    @Test
    void application_requires_a_tenant_id() {
        assertThatThrownBy(() -> new RoleScope(RoleScopeLevel.APPLICATION, Optional.empty(), Optional.of(APPLICATION)))
                .isInstanceOf(InvalidRoleScopeException.class);
    }

    @Test
    void application_requires_an_application_id() {
        assertThatThrownBy(() -> new RoleScope(RoleScopeLevel.APPLICATION, Optional.of(TENANT), Optional.empty()))
                .isInstanceOf(InvalidRoleScopeException.class);
    }

    @Test
    void is_global_is_true_only_for_the_global_level() {
        assertThat(RoleScope.global().isGlobal()).isTrue();
        assertThat(RoleScope.ofTenant(TENANT).isGlobal()).isFalse();
        assertThat(RoleScope.ofApplication(TENANT, APPLICATION).isGlobal()).isFalse();
    }
}
