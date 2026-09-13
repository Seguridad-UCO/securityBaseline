package co.edu.uco.seguridad.pdp.assignments.domain.rule.impl;

import co.edu.uco.seguridad.pdp.assignments.domain.exception.ProfileAssignmentNotFoundException;
import co.edu.uco.seguridad.pdp.assignments.domain.model.ProfileAssignmentId;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.model.ProfileAssignmentExistence;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileAssignmentMustExistForTenantRuleImplTests {

    private static final ProfileAssignmentId ID = new ProfileAssignmentId(UUID.randomUUID());
    private static final TenantId TENANT = new TenantId("universidad-uco");

    @Test
    void rejects_when_not_registered() {
        ProfileAssignmentMustExistForTenantRuleImpl rule = new ProfileAssignmentMustExistForTenantRuleImpl();

        assertThatThrownBy(() -> rule.execute(new ProfileAssignmentExistence(ID, TENANT, false)))
                .isInstanceOf(ProfileAssignmentNotFoundException.class);
    }

    @Test
    void accepts_when_registered() {
        ProfileAssignmentMustExistForTenantRuleImpl rule = new ProfileAssignmentMustExistForTenantRuleImpl();

        assertThatCode(() -> rule.execute(new ProfileAssignmentExistence(ID, TENANT, true)))
                .doesNotThrowAnyException();
    }
}
