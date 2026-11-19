package co.edu.uco.seguridad.pdp.identity.domain.rule.impl;

import co.edu.uco.seguridad.pdp.identity.domain.exception.UserEmailAlreadyRegisteredException;
import co.edu.uco.seguridad.pdp.identity.domain.model.Email;
import co.edu.uco.seguridad.pdp.identity.domain.rule.UserEmailMustBeAvailableRule;
import co.edu.uco.seguridad.pdp.identity.domain.rule.model.UserEmailAvailability;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserEmailMustBeAvailableRuleImplTests {

    private static final Email EMAIL = new Email("david@uco.edu.co");

    private final UserEmailMustBeAvailableRule rule = new UserEmailMustBeAvailableRuleImpl();

    @Test
    void accepts_an_email_that_no_user_has_yet() {
        assertThatCode(() -> rule.execute(new UserEmailAvailability(EMAIL, false))).doesNotThrowAnyException();
    }

    @Test
    void refuses_an_email_that_already_belongs_to_a_user() {
        assertThatThrownBy(() -> rule.execute(new UserEmailAvailability(EMAIL, true)))
                .isInstanceOf(UserEmailAlreadyRegisteredException.class);
    }
}
