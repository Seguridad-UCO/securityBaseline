package co.edu.uco.seguridad.pdp.identity.domain.rule;

import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.domain.exception.UserNotFoundException;
import co.edu.uco.seguridad.pdp.identity.domain.rule.impl.UserMustExistRuleImpl;
import co.edu.uco.seguridad.pdp.identity.domain.rule.model.UserExistence;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserMustExistRuleTests {

    private static final UserId ID = new UserId(UUID.randomUUID());

    @Test
    void accepts_a_registered_user() {
        UserMustExistRule rule = new UserMustExistRuleImpl();

        assertThatCode(() -> rule.execute(new UserExistence(ID, true))).doesNotThrowAnyException();
    }

    @Test
    void refuses_a_user_that_does_not_exist() {
        UserMustExistRule rule = new UserMustExistRuleImpl();

        assertThatThrownBy(() -> rule.execute(new UserExistence(ID, false)))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(ID.value().toString());
    }
}
