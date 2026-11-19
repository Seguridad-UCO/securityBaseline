package co.edu.uco.seguridad.pdp.roles.domain.rule.impl;

import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.roles.domain.exception.RoleNotFoundException;
import co.edu.uco.seguridad.pdp.roles.domain.rule.model.RoleExistence;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleMustExistForTenantRuleImplTests {

    private static final RoleId ROLE = new RoleId(UUID.randomUUID());
    private static final TenantId TENANT = new TenantId("universidad-uco");

    @Test
    void rejects_a_role_not_registered_for_that_tenant() {
        RoleMustExistForTenantRuleImpl rule = new RoleMustExistForTenantRuleImpl();

        assertThatThrownBy(() -> rule.execute(new RoleExistence(ROLE, TENANT, false)))
                .isInstanceOf(RoleNotFoundException.class);
    }

    @Test
    void accepts_a_role_registered_for_that_tenant() {
        RoleMustExistForTenantRuleImpl rule = new RoleMustExistForTenantRuleImpl();

        assertThatCode(() -> rule.execute(new RoleExistence(ROLE, TENANT, true))).doesNotThrowAnyException();
    }
}
