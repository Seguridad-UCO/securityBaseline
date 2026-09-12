package co.edu.uco.seguridad.pdp.resources.domain.rule;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.rule.model.ProtectedResourceAvailability;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import co.edu.uco.seguridad.pdp.resources.domain.exception.DuplicateProtectedResourceException;
import co.edu.uco.seguridad.pdp.resources.domain.rule.impl.ProtectedResourceMustBeUniqueRuleImpl;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProtectedResourceMustBeUniqueRuleTests {

    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());

    @Test
    void accepts_an_endpoint_not_registered_yet() {
        ProtectedResourceMustBeUniqueRule rule = new ProtectedResourceMustBeUniqueRuleImpl();

        assertThatCode(() -> rule.execute(availability(false))).doesNotThrowAnyException();
    }

    @Test
    void refuses_an_endpoint_already_registered() {
        ProtectedResourceMustBeUniqueRule rule = new ProtectedResourceMustBeUniqueRuleImpl();

        assertThatThrownBy(() -> rule.execute(availability(true)))
                .isInstanceOf(DuplicateProtectedResourceException.class)
                .hasMessageContaining("/estudiantes");
    }

    private static ProtectedResourceAvailability availability(boolean alreadyRegistered) {
        return new ProtectedResourceAvailability(APPLICATION, new ResourcePath("/estudiantes"), HttpVerb.GET,
                alreadyRegistered);
    }
}
