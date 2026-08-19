package co.edu.uco.seguridad.pdp.resources.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.port.primary.dto.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.pdp.applications.application.port.primary.dto.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RegisterApplicationUseCase;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RemoveApplicationUseCase;
import co.edu.uco.seguridad.pdp.commons.AggregateRoot;
import co.edu.uco.seguridad.pdp.commons.ResourceId;
import co.edu.uco.seguridad.pdp.resources.application.model.ProtectedResourceRegistration;
import co.edu.uco.seguridad.pdp.resources.application.port.primary.dto.request.RegisterProtectedApplicationRequest;
import co.edu.uco.seguridad.pdp.resources.application.port.secondary.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.application.rulesvalidator.RegisterProtectedApplicationRulesValidator;
import co.edu.uco.seguridad.pdp.resources.application.usecase.RegisterProtectedApplicationUseCase;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.resources.domain.event.ProtectedResourceRegistered;
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
 * Orquesta E-1 como saga con compensación explícita por paso — ver ADR-019. Cruza el módulo
 * {@code applications} (crea la aplicación, luego el recurso), así que no puede resolverse como una
 * única transacción SurrealQL sin que este módulo conozca el esquema de persistencia de
 * {@code applications} — exactamente el acoplamiento que Modulith impide en otro lado
 * ({@code allowedDependencies} solo expone {@code usecase}/{@code dto}/{@code exception}, nunca
 * el repositorio). La compensación registra su propio fallo y siempre repropaga el error original,
 * nunca el de la compensación — perder ese rastro dejaría el registro huérfano sin ninguna pista de
 * por qué.
 */
public final class RegisterProtectedApplicationUseCaseImpl implements RegisterProtectedApplicationUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterProtectedApplicationUseCaseImpl.class);

    private final RegisterApplicationUseCase registerApplicationUseCase;
    private final RemoveApplicationUseCase removeApplicationUseCase;
    private final RegisterProtectedApplicationRulesValidator rules;
    private final ProtectedResourceRepository resources;
    private final DomainEventPublisher events;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public RegisterProtectedApplicationUseCaseImpl(RegisterApplicationUseCase registerApplicationUseCase,
                                                    RemoveApplicationUseCase removeApplicationUseCase,
                                                    RegisterProtectedApplicationRulesValidator rules,
                                                    ProtectedResourceRepository resources,
                                                    DomainEventPublisher events,
                                                    IdentifierGenerator identifiers,
                                                    TimeProvider time) {
        this.registerApplicationUseCase = Objects.requireNonNull(
                registerApplicationUseCase, RequiredArgumentMessages.REGISTER_USE_CASE);
        this.removeApplicationUseCase = Objects.requireNonNull(
                removeApplicationUseCase, RequiredArgumentMessages.REMOVE_USE_CASE);
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
                        .onErrorResume(error -> compensateApplication(application, error).then(Mono.error(error))))
                .transform(ReactiveLogContext.withContext(LOG, "protected_application.register"));
    }

    private Mono<RegisteredApplicationResponse> registerApplication(RegisterProtectedApplicationRequest dto) {
        return registerApplicationUseCase.execute(
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
                                .onErrorResume(error -> compensateResource(saved, error).then(Mono.error(error)))));
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

    /**
     * {@code originalError} es lo que el cliente termina viendo — la compensación nunca lo
     * reemplaza. Si la compensación misma falla, ese segundo error solo se registra: propagarlo
     * ocultaría la causa real (por ejemplo "recurso duplicado") detrás de un fallo de limpieza, y el
     * cliente no puede hacer nada con "no se pudo compensar" que no pueda hacer con la causa real.
     */
    private Mono<Void> compensateResource(ProtectedResource saved, Throwable originalError) {
        return resources.deleteById(saved.id())
                .onErrorResume(compensationError -> {
                    LOG.error("no se pudo compensar el recurso protegido {} tras fallo previo ({}) — queda huérfano "
                                    + "en SurrealDB, requiere limpieza manual",
                            saved.id(), originalError.toString(), compensationError);
                    return Mono.empty();
                });
    }

    private Mono<Void> compensateApplication(RegisteredApplicationResponse application, Throwable originalError) {
        return removeApplicationUseCase.execute(application.id())
                .onErrorResume(compensationError -> {
                    LOG.error("no se pudo compensar la aplicación {} tras fallo previo ({}) — queda huérfana en "
                                    + "SurrealDB, requiere limpieza manual",
                            application.id(), originalError.toString(), compensationError);
                    return Mono.empty();
                });
    }
}
