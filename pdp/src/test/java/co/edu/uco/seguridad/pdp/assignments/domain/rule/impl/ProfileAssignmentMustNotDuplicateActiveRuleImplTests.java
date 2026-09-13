package co.edu.uco.seguridad.pdp.assignments.domain.rule.impl;

import co.edu.uco.seguridad.pdp.assignments.domain.exception.DuplicateProfileAssignmentException;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.model.ActiveProfileAssignmentAvailability;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileAssignmentMustNotDuplicateActiveRuleImplTests {

    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final ProfileId PROFILE = new ProfileId(UUID.randomUUID());

    @Test
    void rejects_when_there_is_already_an_active_profile_assignment() {
        ProfileAssignmentMustNotDuplicateActiveRuleImpl rule = new ProfileAssignmentMustNotDuplicateActiveRuleImpl();

        assertThatThrownBy(() -> rule.execute(new ActiveProfileAssignmentAvailability(USER, APPLICATION, PROFILE, true)))
                .isInstanceOf(DuplicateProfileAssignmentException.class);
    }

    @Test
    void accepts_when_there_is_no_active_profile_assignment() {
        ProfileAssignmentMustNotDuplicateActiveRuleImpl rule = new ProfileAssignmentMustNotDuplicateActiveRuleImpl();

        assertThatCode(() -> rule.execute(new ActiveProfileAssignmentAvailability(USER, APPLICATION, PROFILE, false)))
                .doesNotThrowAnyException();
    }
}
