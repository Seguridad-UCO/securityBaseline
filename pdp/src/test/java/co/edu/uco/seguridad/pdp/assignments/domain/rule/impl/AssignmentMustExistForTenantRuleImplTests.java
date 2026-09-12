package co.edu.uco.seguridad.pdp.assignments.domain.rule.impl;

import co.edu.uco.seguridad.pdp.assignments.domain.exception.AssignmentNotFoundException;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.model.AssignmentExistence;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssignmentMustExistForTenantRuleImplTests {

    private static final AssignmentId ID = new AssignmentId(UUID.randomUUID());
    private static final TenantId TENANT = new TenantId("universidad-uco");

    @Test
    void rejects_when_not_registered() {
        AssignmentMustExistForTenantRuleImpl rule = new AssignmentMustExistForTenantRuleImpl();

        assertThatThrownBy(() -> rule.execute(new AssignmentExistence(ID, TENANT, false)))
                .isInstanceOf(AssignmentNotFoundException.class);
    }

    @Test
    void accepts_when_registered() {
        AssignmentMustExistForTenantRuleImpl rule = new AssignmentMustExistForTenantRuleImpl();

        assertThatCode(() -> rule.execute(new AssignmentExistence(ID, TENANT, true)))
                .doesNotThrowAnyException();
    }
}
