package co.edu.uco.seguridad.pep.normalization.application.usecase.impl;

import co.edu.uco.seguridad.pep.commons.AccessRequest;
import co.edu.uco.seguridad.pep.normalization.application.port.primary.dto.request.NormalizeAccessRequest;
import co.edu.uco.seguridad.pep.normalization.application.rulesvalidator.NormalizeAccessRulesValidator;
import co.edu.uco.seguridad.pep.normalization.application.usecase.NormalizeAccessUseCase;
import reactor.core.publisher.Mono;

public final class NormalizeAccessUseCaseImpl implements NormalizeAccessUseCase {
    private final NormalizeAccessRulesValidator rules;

    public NormalizeAccessUseCaseImpl(NormalizeAccessRulesValidator rules) {
        this.rules = rules;
    }

    @Override
    public Mono<AccessRequest> execute(NormalizeAccessRequest input) {
        return rules.execute(input).then(Mono.fromSupplier(() -> new AccessRequest("1", input.requestId(), input.correlationId(),
                input.timestamp(), new AccessRequest.Application(input.applicationId(), input.environment()),
                new AccessRequest.Resource(input.path(), input.method()),
                new AccessRequest.Context(input.method(), "HTTP"))));
    }

}
