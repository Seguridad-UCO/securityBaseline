package co.edu.uco.seguridad.pdp.roles.domain.rule.impl;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.roles.domain.exception.ResourceOutsideRoleScopeException;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.pdp.roles.domain.rule.model.ResourceCoverage;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regla R5 (INV-DAT-01): global cubre todo; tenant cubre sus aplicaciones; aplicación cubre solo la suya.
 */
class RoleScopeMustCoverResourceRuleImplTests {

    private static final TenantId UCO = new TenantId("universidad-uco");
    private static final TenantId OTRO = new TenantId("otra-universidad");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final ApplicationId OTHER_APPLICATION = new ApplicationId(UUID.randomUUID());

    @Test
    void a_global_role_covers_any_resource() {
        RoleScopeMustCoverResourceRuleImpl rule = new RoleScopeMustCoverResourceRuleImpl();

        assertThatCode(() -> rule.execute(new ResourceCoverage(RoleScope.global(), APPLICATION, OTRO)))
                .doesNotThrowAnyException();
    }

    @Test
    void a_tenant_role_covers_a_resource_of_its_own_tenant() {
        RoleScopeMustCoverResourceRuleImpl rule = new RoleScopeMustCoverResourceRuleImpl();

        assertThatCode(() -> rule.execute(new ResourceCoverage(RoleScope.ofTenant(UCO), APPLICATION, UCO)))
                .doesNotThrowAnyException();
    }

    @Test
    void a_tenant_role_does_not_cover_a_resource_of_another_tenant() {
        RoleScopeMustCoverResourceRuleImpl rule = new RoleScopeMustCoverResourceRuleImpl();

        assertThatThrownBy(() -> rule.execute(new ResourceCoverage(RoleScope.ofTenant(UCO), APPLICATION, OTRO)))
                .isInstanceOf(ResourceOutsideRoleScopeException.class);
    }

    @Test
    void an_application_role_covers_a_resource_of_its_own_application() {
        RoleScopeMustCoverResourceRuleImpl rule = new RoleScopeMustCoverResourceRuleImpl();
        RoleScope scope = RoleScope.ofApplication(UCO, APPLICATION);

        assertThatCode(() -> rule.execute(new ResourceCoverage(scope, APPLICATION, UCO))).doesNotThrowAnyException();
    }

    @Test
    void an_application_role_does_not_cover_a_resource_of_another_application() {
        RoleScopeMustCoverResourceRuleImpl rule = new RoleScopeMustCoverResourceRuleImpl();
        RoleScope scope = RoleScope.ofApplication(UCO, APPLICATION);

        assertThatThrownBy(() -> rule.execute(new ResourceCoverage(scope, OTHER_APPLICATION, UCO)))
                .isInstanceOf(ResourceOutsideRoleScopeException.class);
    }
}
