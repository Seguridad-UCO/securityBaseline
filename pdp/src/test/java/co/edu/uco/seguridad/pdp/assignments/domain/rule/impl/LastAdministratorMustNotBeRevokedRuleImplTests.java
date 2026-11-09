package co.edu.uco.seguridad.pdp.assignments.domain.rule.impl;

import co.edu.uco.seguridad.pdp.assignments.domain.exception.CannotRemoveLastAdministratorException;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.model.AdministratorRevocationEligibility;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LastAdministratorMustNotBeRevokedRuleImplTests {

    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());

    @Test
    void accepts_when_there_is_more_than_one_active_administrator() {
        LastAdministratorMustNotBeRevokedRuleImpl rule = new LastAdministratorMustNotBeRevokedRuleImpl();

        assertThatCode(() -> rule.execute(new AdministratorRevocationEligibility(APPLICATION, 2)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejects_when_it_is_the_only_active_administrator() {
        LastAdministratorMustNotBeRevokedRuleImpl rule = new LastAdministratorMustNotBeRevokedRuleImpl();

        assertThatThrownBy(() -> rule.execute(new AdministratorRevocationEligibility(APPLICATION, 1)))
                .isInstanceOf(CannotRemoveLastAdministratorException.class);
    }

    @Test
    void rejects_when_there_is_no_active_administrator_left() {
        // No debería ocurrir en la práctica (siempre hay al menos uno cuando se intenta revocar),
        // pero documentamos el caso en vez de dejarlo sin cubrir: cero también es "no queda más de uno".
        LastAdministratorMustNotBeRevokedRuleImpl rule = new LastAdministratorMustNotBeRevokedRuleImpl();

        assertThatThrownBy(() -> rule.execute(new AdministratorRevocationEligibility(APPLICATION, 0)))
                .isInstanceOf(CannotRemoveLastAdministratorException.class);
    }
}
