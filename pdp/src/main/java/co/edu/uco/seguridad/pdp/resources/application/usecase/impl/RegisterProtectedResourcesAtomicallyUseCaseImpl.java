package co.edu.uco.seguridad.pdp.resources.application.usecase.impl;

import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.RegisterProtectedResourcesAtomicallyRequest;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.application.usecase.RegisterProtectedResourcesAtomicallyUseCase;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.resources.domain.event.ProtectedResourceRegistered;
import co.edu.uco.seguridad.shared.event.DomainEventPublisher;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

public final class RegisterProtectedResourcesAtomicallyUseCaseImpl
        implements RegisterProtectedResourcesAtomicallyUseCase {
    private final ProtectedResourceRepository resources;
    private final DomainEventPublisher events;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public RegisterProtectedResourcesAtomicallyUseCaseImpl(ProtectedResourceRepository resources,
                                                           DomainEventPublisher events, IdentifierGenerator identifiers,
                                                           TimeProvider time) {
        this.resources = Objects.requireNonNull(resources);
        this.events = Objects.requireNonNull(events);
        this.identifiers = Objects.requireNonNull(identifiers);
        this.time = Objects.requireNonNull(time);
    }

    @Override
    public Mono<Void> execute(RegisterProtectedResourcesAtomicallyRequest input) {
        return Mono.defer(() -> {
            List<ProtectedResource> entities = input.resources().stream().map(request -> ProtectedResource.register(
                    new ResourceId(identifiers.next()), request.applicationId(), request.tenantId(), request.path(),
                    request.method(), time.now())).toList();
            return resources.saveAllAtomically(entities).then(Flux.fromIterable(entities)
                    .concatMap(resource -> events.publish(ProtectedResourceRegistered.of(resource))).then());
        });
    }
}
