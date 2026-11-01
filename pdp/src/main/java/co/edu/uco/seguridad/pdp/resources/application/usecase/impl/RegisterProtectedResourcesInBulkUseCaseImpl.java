package co.edu.uco.seguridad.pdp.resources.application.usecase.impl;

import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.application.usecase.RegisterProtectedResourcesInBulkUseCase;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.resources.domain.event.ProtectedResourceRegistered;
import co.edu.uco.seguridad.shared.event.DomainEventPublisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

public final class RegisterProtectedResourcesInBulkUseCaseImpl implements RegisterProtectedResourcesInBulkUseCase {

    private final ProtectedResourceRepository resources;
    private final DomainEventPublisher events;

    public RegisterProtectedResourcesInBulkUseCaseImpl(ProtectedResourceRepository resources, DomainEventPublisher events) {
        this.resources = Objects.requireNonNull(resources);
        this.events = Objects.requireNonNull(events);
    }

    @Override
    public Mono<Void> execute(List<ProtectedResource> batch) {
        return resources.saveAllAtomically(batch)
                .then(Flux.fromIterable(batch).concatMap(r -> events.publish(ProtectedResourceRegistered.of(r))).then());
    }
}
