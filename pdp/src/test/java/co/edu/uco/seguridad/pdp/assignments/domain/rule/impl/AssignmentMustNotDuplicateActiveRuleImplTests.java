package co.edu.uco.seguridad.pdp.assignments.domain.rule.impl;

import co.edu.uco.seguridad.pdp.assignments.domain.exception.DuplicateAssignmentException;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.model.ActiveAssignmentAvailability;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssignmentMustNotDuplicateActiveRuleImplTests {

    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final RoleId ROLE = new RoleId(UUID.randomUUID());

    @Test
    void rejects_when_there_is_already_an_active_assignment() {
        AssignmentMustNotDuplicateActiveRuleImpl rule = new AssignmentMustNotDuplicateActiveRuleImpl();

        assertThatThrownBy(() -> rule.execute(new ActiveAssignmentAvailability(USER, APPLICATION, ROLE, true)))
                .isInstanceOf(DuplicateAssignmentException.class);
    }

    @Test
    void accepts_when_there_is_no_active_assignment() {
        AssignmentMustNotDuplicateActiveRuleImpl rule = new AssignmentMustNotDuplicateActiveRuleImpl();

        assertThatCode(() -> rule.execute(new ActiveAssignmentAvailability(USER, APPLICATION, ROLE, false)))
                .doesNotThrowAnyException();
    }
}
