package co.edu.uco.seguridad.pep.ingress.application.rule.impl;

import co.edu.uco.seguridad.pep.commons.EnforcementFailure;
import co.edu.uco.seguridad.pep.ingress.application.port.primary.dto.request.RegisterIntegrationRequest;
import co.edu.uco.seguridad.pep.ingress.application.port.secondary.IntegrationRegistrationPort;
import co.edu.uco.seguridad.pep.ingress.application.rule.IntegrationRegistrationMustBeEnabledRule;

import java.util.Objects;

public final class IntegrationRegistrationMustBeEnabledRuleImpl implements IntegrationRegistrationMustBeEnabledRule {

    private final IntegrationRegistrationPort registrations;

    public IntegrationRegistrationMustBeEnabledRuleImpl(IntegrationRegistrationPort registrations) {
        this.registrations = Objects.requireNonNull(registrations);
    }

    @Override
    public void execute(RegisterIntegrationRequest input) {
        if (!registrations.enabled()) {
            throw new EnforcementFailure(EnforcementFailure.Kind.UNAVAILABLE, "INTEGRATION_REGISTRATION_DISABLED");
        }
    }
}
