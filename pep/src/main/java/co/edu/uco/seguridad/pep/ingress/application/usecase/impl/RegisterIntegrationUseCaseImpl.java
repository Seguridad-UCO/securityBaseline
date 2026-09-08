package co.edu.uco.seguridad.pep.ingress.application.usecase.impl;

import co.edu.uco.seguridad.pep.commons.EnforcementFailure;
import co.edu.uco.seguridad.pep.ingress.application.port.primary.dto.request.RegisterIntegrationRequest;
import co.edu.uco.seguridad.pep.ingress.application.port.primary.dto.response.RegisteredIntegrationResponse;
import co.edu.uco.seguridad.pep.ingress.application.port.secondary.IntegrationRegistrationPort;
import co.edu.uco.seguridad.pep.ingress.application.rulesvalidator.RegisterIntegrationRulesValidator;
import co.edu.uco.seguridad.pep.ingress.application.usecase.RegisterIntegrationUseCase;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class RegisterIntegrationUseCaseImpl implements RegisterIntegrationUseCase {
    private final RegisterIntegrationRulesValidator rules;
    private final IntegrationRegistrationPort registrations;

    public RegisterIntegrationUseCaseImpl(RegisterIntegrationRulesValidator rules,
                                          IntegrationRegistrationPort registrations) {
        this.rules = Objects.requireNonNull(rules);
        this.registrations = Objects.requireNonNull(registrations);
    }

    @Override
    public Mono<RegisteredIntegrationResponse> execute(RegisterIntegrationRequest input) {
        return rules.execute(input).then(registrations.register(input))
                .onErrorMap(IllegalArgumentException.class,
                        error -> new EnforcementFailure(EnforcementFailure.Kind.INVALID_REQUEST, "INVALID_INTEGRATION_REQUEST"));
    }
}
