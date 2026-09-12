package co.edu.uco.seguridad.pdp.resources.domain.rule;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.resources.domain.exception.ProtectedResourceNotFoundException;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import co.edu.uco.seguridad.pdp.resources.domain.rule.impl.ProtectedResourceMustExistRuleImpl;
import co.edu.uco.seguridad.pdp.resources.domain.rule.model.ProtectedResourceExistence;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProtectedResourceMustExistRuleTests {

    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final ResourcePath PATH = new ResourcePath("/estudiantes");

    @Test
    void accepts_a_registered_resource() {
        ProtectedResourceMustExistRule rule = new ProtectedResourceMustExistRuleImpl();

        assertThatCode(() -> rule.execute(new ProtectedResourceExistence(APPLICATION, PATH, HttpVerb.GET, true)))
                .doesNotThrowAnyException();
    }

    @Test
    void refuses_a_resource_that_is_not_registered() {
        ProtectedResourceMustExistRule rule = new ProtectedResourceMustExistRuleImpl();

        assertThatThrownBy(() -> rule.execute(new ProtectedResourceExistence(APPLICATION, PATH, HttpVerb.GET, false)))
                .isInstanceOf(ProtectedResourceNotFoundException.class);
    }
}
