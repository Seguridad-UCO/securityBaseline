package co.edu.uco.seguridad.pdp.applications.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.RegisterApplicationRulesValidator;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RegisterApplicationUseCase;
import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.observability.ReactiveLogContext;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Implementación de {@link RegisterApplicationUseCase}: valida reglas, construye la entidad y la
 * persiste. Sin reglas propias — la decisión vive en {@link RegisterApplicationRulesValidator}.
 */
public final class RegisterApplicationUseCaseImpl implements RegisterApplicationUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterApplicationUseCaseImpl.class);

    private final RegisterApplicationRulesValidator rules;
    private final ApplicationRepository repository;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public RegisterApplicationUseCaseImpl(RegisterApplicationRulesValidator rules,
            ApplicationRepository repository, IdentifierGenerator identifiers, TimeProvider time) {
        this.rules = Objects.requireNonNull(rules, RequiredArgumentMessages.RULES_VALIDATOR);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.APPLICATION_REPOSITORY);
        this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<RegisteredApplicationResponse> execute(RegisterApplicationRequest dto) {
        return rules.execute(dto)
                .then(Mono.fromSupplier(() -> Application.register(new ApplicationId(identifiers.next()),
                        dto.tenantId(), dto.name(), dto.description(), dto.baseUrl(), time.now())))
                .flatMap(repository::save)
                .map(RegisterApplicationUseCaseImpl::toRegistered)
                .transform(ReactiveLogContext.withContext(LOG, "application.register"));
    }

    private static RegisteredApplicationResponse toRegistered(Application application) {
        return new RegisteredApplicationResponse(application.id(), application.tenantId(), application.name(),
                application.description(), application.baseUrl(), application.registeredAt());
    }
}
