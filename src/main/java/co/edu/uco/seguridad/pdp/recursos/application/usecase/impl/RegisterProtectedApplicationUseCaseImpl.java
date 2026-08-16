package co.edu.uco.seguridad.pdp.recursos.application.usecase.impl;

import co.edu.uco.seguridad.crosscutting.messages.RequiredArgumentMessages;
import co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.dto.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.dto.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.interactor.RegisterApplicationInteractor;
import co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.interactor.RemoveApplicationInteractor;
import co.edu.uco.seguridad.pdp.commons.AggregateRoot;
import co.edu.uco.seguridad.pdp.commons.ResourceId;
import co.edu.uco.seguridad.pdp.recursos.application.model.ProtectedResourceRegistration;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.request.RegisterProtectedApplicationRequest;
import co.edu.uco.seguridad.pdp.recursos.application.port.secondary.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.recursos.application.rulesvalidator.RegisterProtectedApplicationRulesValidator;
import co.edu.uco.seguridad.pdp.recursos.application.usecase.RegisterProtectedApplicationUseCase;
import co.edu.uco.seguridad.pdp.recursos.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.recursos.domain.event.ProtectedResourceRegistered;
import co.edu.uco.seguridad.shared.event.DomainEventPublisher;
import co.edu.uco.seguridad.shared.observability.ReactiveLogContext;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/** Orquesta E-1 como saga con compensación explícita por paso — ver ADR-019. */
public final class RegisterProtectedApplicationUseCaseImpl implements RegisterProtectedApplicationUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterProtectedApplicationUseCaseImpl.class);

    private final RegisterApplicationInteractor registerApplicationInteractor;
    private final RemoveApplicationInteractor removeApplicationInteractor;
    private final RegisterProtectedApplicationRulesValidator rules;
    private final ProtectedResourceRepository resources;
    private final DomainEventPublisher events;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public RegisterProtectedApplicationUseCaseImpl(RegisterApplicationInteractor registerApplicationInteractor,
                                                    RemoveApplicationInteractor removeApplicationInteractor,
                                                    RegisterProtectedApplicationRulesValidator rules,
                                                    ProtectedResourceRepository resources,
                                                    DomainEventPublisher events,
                                                    IdentifierGenerator identifiers,
                                                    TimeProvider time) {
        this.registerApplicationInteractor = Objects.requireNonNull(
                registerApplicationInteractor, "se requiere interactor de registro de aplicación");
        this.removeApplicationInteractor = Objects.requireNonNull(
                removeApplicationInteractor, "se requiere interactor de eliminación de aplicación");
        this.rules = Objects.requireNonNull(rules, RequiredArgumentMessages.RULES_VALIDATOR);
        this.resources = Objects.requireNonNull(resources, RequiredArgumentMessages.PROTECTED_RESOURCE_REPOSITORY);
        this.events = Objects.requireNonNull(events, RequiredArgumentMessages.DOMAIN_EVENT_PUBLISHER);
        this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<ProtectedResource> execute(RegisterProtectedApplicationRequest dto) {
        return registerApplication(dto)
                .flatMap(application -> registerResource(dto, application)
                        .onErrorResume(error -> compensateApplication(application).then(Mono.error(error))))
                .transform(ReactiveLogContext.withContext(LOG, "protected_application.register"));
    }

    private Mono<RegisteredApplicationResponse> registerApplication(RegisterProtectedApplicationRequest dto) {
        return registerApplicationInteractor.execute(
                new RegisterApplicationRequest(dto.tenantId(), dto.applicationName()));
    }

    private Mono<ProtectedResource> registerResource(RegisterProtectedApplicationRequest dto,
                                                     RegisteredApplicationResponse application) {
        ProtectedResourceRegistration registration = new ProtectedResourceRegistration(dto, application);
        return rules.execute(registration)
                .then(Mono.fromSupplier(() -> buildResource(dto, application)))
                .flatMap(outcome -> resources.save(outcome.entity())
                        .flatMap(saved -> publish(outcome.domainEvents())
                                .thenReturn(saved)
                                .onErrorResume(error -> compensateResource(saved).then(Mono.error(error)))));
    }

    private AggregateRoot<ProtectedResource, ProtectedResourceRegistered> buildResource(
            RegisterProtectedApplicationRequest dto, RegisteredApplicationResponse application) {
        return ProtectedResource.registerWithEvent(
                new ResourceId(identifiers.next()),
                application.id(),
                dto.tenantId(),
                application.name(),
                dto.resourceCode(),
                dto.action(),
                time.now());
    }

    private Mono<Void> publish(List<ProtectedResourceRegistered> domainEvents) {
        return Flux.fromIterable(domainEvents).concatMap(events::publish).then();
    }

    private Mono<Void> compensateResource(ProtectedResource saved) {
        return resources.deleteById(saved.id());
    }

    private Mono<Void> compensateApplication(RegisteredApplicationResponse application) {
        return removeApplicationInteractor.execute(application.id());
    }
}
