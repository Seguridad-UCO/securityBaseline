package co.edu.uco.seguridad.pdp.roles.domain.rule.impl;

import co.edu.uco.seguridad.pdp.roles.domain.exception.DuplicateRoleNameException;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.pdp.roles.domain.rule.model.RoleNameAvailability;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleNameMustBeUniqueInScopeRuleImplTests {

    private static final RoleName NAME = new RoleName("Docente");
    private static final RoleScope SCOPE = RoleScope.global();

    @Test
    void rejects_a_name_already_taken_in_that_scope() {
        RoleNameMustBeUniqueInScopeRuleImpl rule = new RoleNameMustBeUniqueInScopeRuleImpl();

        assertThatThrownBy(() -> rule.execute(new RoleNameAvailability(NAME, SCOPE, true)))
                .isInstanceOf(DuplicateRoleNameException.class);
    }

    @Test
    void accepts_a_name_that_is_free_in_that_scope() {
        RoleNameMustBeUniqueInScopeRuleImpl rule = new RoleNameMustBeUniqueInScopeRuleImpl();

        assertThatCode(() -> rule.execute(new RoleNameAvailability(NAME, SCOPE, false))).doesNotThrowAnyException();
    }
}
