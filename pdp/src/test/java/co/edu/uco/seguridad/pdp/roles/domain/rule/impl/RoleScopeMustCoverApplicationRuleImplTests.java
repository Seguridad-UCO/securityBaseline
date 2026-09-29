package co.edu.uco.seguridad.pdp.roles.domain.rule.impl;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.roles.domain.exception.ApplicationOutsideRoleScopeException;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.pdp.roles.domain.rule.model.ApplicationCoverage;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regla R3 de HU-005 (INV-ASN-02): global cubre todo; tenant cubre sus aplicaciones; aplicación cubre solo la suya.
 */
class RoleScopeMustCoverApplicationRuleImplTests {

    private static final TenantId UCO = new TenantId("universidad-uco");
    private static final TenantId OTRO = new TenantId("otra-universidad");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final ApplicationId OTHER_APPLICATION = new ApplicationId(UUID.randomUUID());

    @Test
    void a_global_role_covers_any_application() {
        RoleScopeMustCoverApplicationRuleImpl rule = new RoleScopeMustCoverApplicationRuleImpl();

        assertThatCode(() -> rule.execute(new ApplicationCoverage(RoleScope.global(), APPLICATION, OTRO)))
                .doesNotThrowAnyException();
    }

    @Test
    void a_tenant_role_covers_an_application_of_its_own_tenant() {
        RoleScopeMustCoverApplicationRuleImpl rule = new RoleScopeMustCoverApplicationRuleImpl();

        assertThatCode(() -> rule.execute(new ApplicationCoverage(RoleScope.ofTenant(UCO), APPLICATION, UCO)))
                .doesNotThrowAnyException();
    }

    @Test
    void a_tenant_role_does_not_cover_an_application_of_another_tenant() {
        RoleScopeMustCoverApplicationRuleImpl rule = new RoleScopeMustCoverApplicationRuleImpl();

        assertThatThrownBy(() -> rule.execute(new ApplicationCoverage(RoleScope.ofTenant(UCO), APPLICATION, OTRO)))
                .isInstanceOf(ApplicationOutsideRoleScopeException.class);
    }

    @Test
    void an_application_role_covers_its_own_application() {
        RoleScopeMustCoverApplicationRuleImpl rule = new RoleScopeMustCoverApplicationRuleImpl();
        RoleScope scope = RoleScope.ofApplication(UCO, APPLICATION);

        assertThatCode(() -> rule.execute(new ApplicationCoverage(scope, APPLICATION, UCO))).doesNotThrowAnyException();
    }

    @Test
    void an_application_role_does_not_cover_another_application() {
        RoleScopeMustCoverApplicationRuleImpl rule = new RoleScopeMustCoverApplicationRuleImpl();
        RoleScope scope = RoleScope.ofApplication(UCO, APPLICATION);

        assertThatThrownBy(() -> rule.execute(new ApplicationCoverage(scope, OTHER_APPLICATION, UCO)))
                .isInstanceOf(ApplicationOutsideRoleScopeException.class);
    }
}
