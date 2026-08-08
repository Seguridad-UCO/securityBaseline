package co.edu.uco.seguridad.pdp.recursos.application.usecase.impl;

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
import co.edu.uco.seguridad.shared.port.ReactiveTransactionPort;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * Orquesta E-1: registrar la aplicación a través de Aplicaciones, luego su primer recurso aquí,
 * como una unidad de trabajo. Devuelve el agregado; el interactor proyecta al DTO.
 */
public final class RegisterProtectedApplicationUseCaseImpl implements RegisterProtectedApplicationUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterProtectedApplicationUseCaseImpl.class);

    private final RegisterApplicationInteractor registerApplicationInteractor;
    private final RemoveApplicationInteractor removeApplicationInteractor;
    private final RegisterProtectedApplicationRulesValidator rules;
    private final ProtectedResourceRepository resources;
    private final DomainEventPublisher events;
    private final ReactiveTransactionPort transaction;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public RegisterProtectedApplicationUseCaseImpl(RegisterApplicationInteractor registerApplicationInteractor,
                                                    RemoveApplicationInteractor removeApplicationInteractor,
                                                    RegisterProtectedApplicationRulesValidator rules,
                                                    ProtectedResourceRepository resources,
                                                    DomainEventPublisher events,
                                                    ReactiveTransactionPort transaction,
                                                    IdentifierGenerator identifiers,
                                                    TimeProvider time) {
        this.registerApplicationInteractor = Objects.requireNonNull(
                registerApplicationInteractor, "se requiere interactor de registro de aplicación");
        this.removeApplicationInteractor = Objects.requireNonNull(
                removeApplicationInteractor, "se requiere interactor de eliminación de aplicación");
        this.rules = Objects.requireNonNull(rules, "se requiere validador de reglas");
        this.resources = Objects.requireNonNull(resources, "se requiere repositorio de recurso protegido");
        this.events = Objects.requireNonNull(events, "se requiere publicador de eventos de dominio");
        this.transaction = Objects.requireNonNull(transaction, "se requiere puerto de transacción");
        this.identifiers = Objects.requireNonNull(identifiers, "se requiere generador de identificadores");
        this.time = Objects.requireNonNull(time, "se requiere proveedor de tiempo");
    }

    @Override
    public Mono<ProtectedResource> execute(RegisterProtectedApplicationRequest dto) {
        return transaction.execute(() -> registerApplication(dto)
                        .flatMap(application -> registerResource(dto, application)
                                .onErrorResume(error -> compensateApplication(application).then(Mono.error(error)))))
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
                        .flatMap(saved -> publish(outcome.domainEvents()).thenReturn(saved)));
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

    private Mono<Void> compensateApplication(RegisteredApplicationResponse application) {
        return removeApplicationInteractor.execute(application.id());
    }
}
