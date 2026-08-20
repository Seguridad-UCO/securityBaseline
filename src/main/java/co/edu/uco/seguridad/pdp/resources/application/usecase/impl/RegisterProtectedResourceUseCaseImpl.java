package co.edu.uco.seguridad.pdp.resources.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.exception.ApplicationNotFoundException;
import co.edu.uco.seguridad.pdp.applications.application.port.secondary.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.commons.ResourceId;
import co.edu.uco.seguridad.pdp.resources.application.port.primary.dto.request.RegisterProtectedResourceRequest;
import co.edu.uco.seguridad.pdp.resources.application.port.primary.dto.response.RegisteredProtectedResourceResponse;
import co.edu.uco.seguridad.pdp.resources.application.port.secondary.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.application.rulesvalidator.RegisterProtectedResourceRulesValidator;
import co.edu.uco.seguridad.pdp.resources.application.usecase.RegisterProtectedResourceUseCase;
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
 * Implementación de {@link RegisterProtectedResourceUseCase}. Confirma que la aplicación exista y
 * pertenezca al inquilino que la referencia antes de delegar en {@link RegisterProtectedResourceRulesValidator}
 * — esa confirmación necesita el repositorio de aplicaciones, así que no es una regla del módulo
 * {@code resources}.
 */
public final class RegisterProtectedResourceUseCaseImpl implements RegisterProtectedResourceUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterProtectedResourceUseCaseImpl.class);

    private final ApplicationRepository applications;
    private final RegisterProtectedResourceRulesValidator rules;
    private final ProtectedResourceRepository resources;
    private final DomainEventPublisher events;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public RegisterProtectedResourceUseCaseImpl(ApplicationRepository applications,
            RegisterProtectedResourceRulesValidator rules, ProtectedResourceRepository resources,
            DomainEventPublisher events, IdentifierGenerator identifiers, TimeProvider time) {
        this.applications = Objects.requireNonNull(applications, RequiredArgumentMessages.APPLICATION_REPOSITORY);
        this.rules = Objects.requireNonNull(rules, RequiredArgumentMessages.RULES_VALIDATOR);
        this.resources = Objects.requireNonNull(resources, RequiredArgumentMessages.PROTECTED_RESOURCE_REPOSITORY);
        this.events = Objects.requireNonNull(events, RequiredArgumentMessages.DOMAIN_EVENT_PUBLISHER);
        this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<RegisteredProtectedResourceResponse> execute(RegisterProtectedResourceRequest dto) {
        return applications.findByTenantAndId(dto.tenantId(), dto.applicationId())
                .switchIfEmpty(Mono.error(() -> new ApplicationNotFoundException(dto.applicationId())))
                .then(rules.execute(dto))
                .then(Mono.fromSupplier(() -> ProtectedResource.registerWithEvent(new ResourceId(identifiers.next()),
                        dto.applicationId(), dto.tenantId(), dto.path(), dto.method(), time.now())))
                .flatMap(outcome -> resources.save(outcome.entity())
                        .flatMap(saved -> publish(outcome.domainEvents()).thenReturn(saved)))
                .map(RegisterProtectedResourceUseCaseImpl::toRegistered)
                .transform(ReactiveLogContext.withContext(LOG, "resource.register"));
    }

    private Mono<Void> publish(List<ProtectedResourceRegistered> domainEvents) {
        return Flux.fromIterable(domainEvents).concatMap(events::publish).then();
    }

    private static RegisteredProtectedResourceResponse toRegistered(ProtectedResource resource) {
        return new RegisteredProtectedResourceResponse(resource.id(), resource.applicationId(), resource.tenantId(),
                resource.path(), resource.method(), resource.registeredAt());
    }
}
