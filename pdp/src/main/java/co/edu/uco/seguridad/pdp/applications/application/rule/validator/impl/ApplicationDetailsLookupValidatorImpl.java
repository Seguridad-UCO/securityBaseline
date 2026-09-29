package co.edu.uco.seguridad.pdp.applications.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationDetailsLookupValidator;
import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.domain.exception.ApplicationNotFoundException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Adaptador de la consulta de detalle sin exponer el agregado Application fuera de su módulo.
 */
public final class ApplicationDetailsLookupValidatorImpl implements ApplicationDetailsLookupValidator {
    private final ApplicationRepository repository;

    public ApplicationDetailsLookupValidatorImpl(ApplicationRepository repository) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.APPLICATION_REPOSITORY);
    }

    @Override
    public Mono<RegisteredApplicationResponse> execute(ApplicationId id) {
        return repository.findTenantIdById(id).flatMap(tenantId -> repository.findByIdForTenant(tenantId, id)
                        .map(application -> new RegisteredApplicationResponse(application.id(), application.tenantId(), application.name(), application.description(), application.baseUrl(), application.registeredAt())))
                .switchIfEmpty(Mono.error(() -> new ApplicationNotFoundException(id)));
    }
}
