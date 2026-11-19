package co.edu.uco.seguridad.pdp.applications.domain.rule;

import co.edu.uco.seguridad.pdp.applications.domain.exception.InvalidApplicationCredentialException;
import co.edu.uco.seguridad.pdp.applications.domain.rule.impl.ApplicationCredentialMustBeValidRuleImpl;
import co.edu.uco.seguridad.pdp.applications.domain.rule.model.ApplicationCredentialValidity;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationCredentialMustBeValidRuleImplTests {

    private static final ApplicationId ID = new ApplicationId(UUID.randomUUID());

    @Test
    void accepts_a_valid_credential() {
        ApplicationCredentialMustBeValidRule rule = new ApplicationCredentialMustBeValidRuleImpl();

        assertThatCode(() -> rule.execute(new ApplicationCredentialValidity(ID, true))).doesNotThrowAnyException();
    }

    @Test
    void refuses_an_invalid_credential() {
        ApplicationCredentialMustBeValidRule rule = new ApplicationCredentialMustBeValidRuleImpl();

        assertThatThrownBy(() -> rule.execute(new ApplicationCredentialValidity(ID, false)))
                .isInstanceOf(InvalidApplicationCredentialException.class);
    }
}
