package co.edu.uco.seguridad.pep.ingress.application.rule.impl;

import co.edu.uco.seguridad.pep.commons.EnforcementFailure;
import co.edu.uco.seguridad.pep.ingress.application.port.primary.dto.request.RegisterIntegrationRequest;
import co.edu.uco.seguridad.pep.ingress.application.port.secondary.ApplicationCredentialValidationPort;
import co.edu.uco.seguridad.pep.ingress.application.rule.IntegrationCredentialMustMatchRule;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class IntegrationCredentialMustMatchRuleImpl implements IntegrationCredentialMustMatchRule {

    private final ApplicationCredentialValidationPort credentials;

    public IntegrationCredentialMustMatchRuleImpl(ApplicationCredentialValidationPort credentials) {
        this.credentials = Objects.requireNonNull(credentials);
    }

    @Override
    public Mono<Void> execute(RegisterIntegrationRequest input) {
        if (input == null || input.bearerToken() == null || input.bearerToken().isBlank())
            return Mono.error(new EnforcementFailure(EnforcementFailure.Kind.UNAUTHENTICATED, "INTEGRATION_TOKEN_INVALID"));
        return credentials.validate(input.applicationId(), input.bearerToken());
    }
}
