package co.edu.uco.seguridad.pdp.authorization.domain.rule.impl;

import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerTenantCatalogException;
import co.edu.uco.seguridad.pdp.authorization.domain.rule.model.UserTenantAssignment;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTenantAssignmentMustStayInActorTenantRuleImplTests {

    private static final TenantId ACTOR = new TenantId("universidad-uco");
    private static final TenantId OTHER = new TenantId("colegio-otro");

    private final UserTenantAssignmentMustStayInActorTenantRuleImpl rule = new UserTenantAssignmentMustStayInActorTenantRuleImpl();

    @Test
    void a_user_of_the_actor_tenant_stays_in_the_actor_tenant() {
        assertThatCode(() -> rule.execute(new UserTenantAssignment(ACTOR, ACTOR, ACTOR))).doesNotThrowAnyException();
    }

    @Test
    void a_user_of_another_tenant_cannot_be_touched() {
        assertThatThrownBy(() -> rule.execute(new UserTenantAssignment(ACTOR, OTHER, ACTOR)))
                .isInstanceOf(NotAuthorizedToAdministerTenantCatalogException.class);
    }

    @Test
    void a_user_cannot_be_moved_out_of_the_actor_tenant() {
        assertThatThrownBy(() -> rule.execute(new UserTenantAssignment(ACTOR, ACTOR, OTHER)))
                .isInstanceOf(NotAuthorizedToAdministerTenantCatalogException.class);
    }

    @Test
    void a_user_cannot_be_pulled_into_the_actor_tenant_from_another() {
        assertThatThrownBy(() -> rule.execute(new UserTenantAssignment(ACTOR, OTHER, OTHER)))
                .isInstanceOf(NotAuthorizedToAdministerTenantCatalogException.class);
    }
}
