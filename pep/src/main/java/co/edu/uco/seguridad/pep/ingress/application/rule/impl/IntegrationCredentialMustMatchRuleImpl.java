package co.edu.uco.seguridad.pep.ingress.application.rule.impl;

import co.edu.uco.seguridad.pep.commons.EnforcementFailure;
import co.edu.uco.seguridad.pep.ingress.application.port.primary.dto.request.RegisterIntegrationRequest;
import co.edu.uco.seguridad.pep.ingress.application.port.secondary.IntegrationRegistrationPort;
import co.edu.uco.seguridad.pep.ingress.application.rule.IntegrationCredentialMustMatchRule;

import java.util.Objects;

public final class IntegrationCredentialMustMatchRuleImpl implements IntegrationCredentialMustMatchRule {

    private final IntegrationRegistrationPort registrations;

    public IntegrationCredentialMustMatchRuleImpl(IntegrationRegistrationPort registrations) {
        this.registrations = Objects.requireNonNull(registrations);
    }

    @Override
    public void execute(RegisterIntegrationRequest input) {
        if (input == null || !registrations.credentialMatches(input.applicationId(), input.environment(), input.bearerToken())) {
            throw new EnforcementFailure(EnforcementFailure.Kind.UNAUTHENTICATED, "INTEGRATION_TOKEN_INVALID");
        }
    }
}
