package co.edu.uco.seguridad.pdp.applications.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.port.primary.dto.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.pdp.applications.application.port.primary.dto.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.applications.application.port.secondary.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.application.rulesvalidator.RegisterApplicationRulesValidator;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RegisterApplicationUseCase;
import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.domain.event.ApplicationRegistered;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.shared.event.DomainEventPublisher;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.observability.ReactiveLogContext;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * Implementación de {@link RegisterApplicationUseCase}: valida reglas, construye la entidad, la
 * persiste y proyecta el resultado. Sin reglas propias — la decisión vive en
 * {@link RegisterApplicationRulesValidator}.
 */
public final class RegisterApplicationUseCaseImpl implements RegisterApplicationUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterApplicationUseCaseImpl.class);

    private final RegisterApplicationRulesValidator rules;
    private final ApplicationRepository repository;
    private final DomainEventPublisher events;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public RegisterApplicationUseCaseImpl(RegisterApplicationRulesValidator rules,
                                          ApplicationRepository repository,
                                          DomainEventPublisher events,
                                          IdentifierGenerator identifiers,
                                          TimeProvider time) {
        this.rules = Objects.requireNonNull(rules, RequiredArgumentMessages.RULES_VALIDATOR);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.APPLICATION_REPOSITORY);
        this.events = Objects.requireNonNull(events, RequiredArgumentMessages.DOMAIN_EVENT_PUBLISHER);
        this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<RegisteredApplicationResponse> execute(RegisterApplicationRequest dto) {
        return rules.execute(dto)
                .then(Mono.fromSupplier(() -> Application.registerWithEvent(
                        new ApplicationId(identifiers.next()), dto.tenantId(), dto.name(), time.now())))
                .flatMap(outcome -> repository.save(outcome.entity())
                        .flatMap(saved -> publish(outcome.domainEvents()).thenReturn(saved)))
                .map(RegisterApplicationUseCaseImpl::toRegistered)
                .transform(ReactiveLogContext.withContext(LOG, "application.register"));
    }

    private Mono<Void> publish(List<ApplicationRegistered> domainEvents) {
        return Flux.fromIterable(domainEvents).concatMap(events::publish).then();
    }

    private static RegisteredApplicationResponse toRegistered(Application application) {
        return new RegisteredApplicationResponse(
                application.id(), application.tenantId(), application.name(), application.registeredAt());
    }
}
