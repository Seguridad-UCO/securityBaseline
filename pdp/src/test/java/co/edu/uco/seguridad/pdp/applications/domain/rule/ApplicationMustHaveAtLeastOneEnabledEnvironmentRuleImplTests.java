package co.edu.uco.seguridad.pdp.applications.domain.rule;

import co.edu.uco.seguridad.pdp.applications.domain.exception.ApplicationWithoutEnabledEnvironmentException;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationEnvironment;
import co.edu.uco.seguridad.pdp.applications.domain.rule.impl.ApplicationMustHaveAtLeastOneEnabledEnvironmentRuleImpl;
import co.edu.uco.seguridad.pdp.applications.domain.rule.model.ApplicationEnvironmentSelection;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationMustHaveAtLeastOneEnabledEnvironmentRuleImplTests {

    private final ApplicationMustHaveAtLeastOneEnabledEnvironmentRule rule =
            new ApplicationMustHaveAtLeastOneEnabledEnvironmentRuleImpl();

    @Test
    void allows_a_selection_with_an_enabled_environment() {
        assertThatCode(() -> rule.execute(new ApplicationEnvironmentSelection(Set.of(ApplicationEnvironment.DEV))))
                .doesNotThrowAnyException();
    }

    @Test
    void rejects_an_empty_selection() {
        assertThatThrownBy(() -> rule.execute(new ApplicationEnvironmentSelection(Set.of())))
                .isInstanceOf(ApplicationWithoutEnabledEnvironmentException.class)
                .hasMessageContaining("al menos un ambiente");
    }

    @Test
    void normalizes_a_null_selection_to_empty_and_rejects_it() {
        assertThatThrownBy(() -> rule.execute(new ApplicationEnvironmentSelection(null)))
                .isInstanceOf(ApplicationWithoutEnabledEnvironmentException.class);
    }
}
