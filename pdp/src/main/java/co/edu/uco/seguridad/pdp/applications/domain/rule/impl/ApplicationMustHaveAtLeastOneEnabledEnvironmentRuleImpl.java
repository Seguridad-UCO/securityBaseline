package co.edu.uco.seguridad.pdp.applications.domain.rule.impl;

import co.edu.uco.seguridad.pdp.applications.domain.exception.ApplicationWithoutEnabledEnvironmentException;
import co.edu.uco.seguridad.pdp.applications.domain.rule.ApplicationMustHaveAtLeastOneEnabledEnvironmentRule;
import co.edu.uco.seguridad.pdp.applications.domain.rule.model.ApplicationEnvironmentSelection;

/** Decisión pura sobre una selección de ambientes ya normalizada. */
public final class ApplicationMustHaveAtLeastOneEnabledEnvironmentRuleImpl
        implements ApplicationMustHaveAtLeastOneEnabledEnvironmentRule {

    @Override
    public void execute(ApplicationEnvironmentSelection selection) {
        if (selection.enabledEnvironments().isEmpty()) {
            throw new ApplicationWithoutEnabledEnvironmentException();
        }
    }
}
