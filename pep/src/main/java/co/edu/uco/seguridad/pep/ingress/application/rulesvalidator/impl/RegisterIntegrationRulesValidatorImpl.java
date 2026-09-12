package co.edu.uco.seguridad.pep.ingress.application.rulesvalidator.impl;

import co.edu.uco.seguridad.pep.ingress.application.port.primary.dto.request.RegisterIntegrationRequest;
import co.edu.uco.seguridad.pep.ingress.application.rule.IntegrationCredentialMustMatchRule;
import co.edu.uco.seguridad.pep.ingress.application.rule.IntegrationRegistrationMustBeEnabledRule;
import co.edu.uco.seguridad.pep.ingress.application.rulesvalidator.RegisterIntegrationRulesValidator;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class RegisterIntegrationRulesValidatorImpl implements RegisterIntegrationRulesValidator {
    private final IntegrationRegistrationMustBeEnabledRule registrationMustBeEnabled;
    private final IntegrationCredentialMustMatchRule credentialMustMatch;

    public RegisterIntegrationRulesValidatorImpl(IntegrationRegistrationMustBeEnabledRule registrationMustBeEnabled,
                                                 IntegrationCredentialMustMatchRule credentialMustMatch) {
        this.registrationMustBeEnabled = Objects.requireNonNull(registrationMustBeEnabled);
        this.credentialMustMatch = Objects.requireNonNull(credentialMustMatch);
    }

    @Override
    public Mono<Void> execute(RegisterIntegrationRequest input) {
        return Mono.fromRunnable(() -> registrationMustBeEnabled.execute(input))
                .then(Mono.fromRunnable(() -> credentialMustMatch.execute(input)));
    }
}
