package co.edu.uco.seguridad.pdp.applications.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.domain.exception.ApplicationNotFoundException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class ApplicationOwnerLookupValidatorImpl implements ApplicationOwnerLookupValidator {

    private final ApplicationRepository repository;

    public ApplicationOwnerLookupValidatorImpl(ApplicationRepository repository) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.APPLICATION_REPOSITORY);
    }

    @Override
    public Mono<TenantId> execute(ApplicationId input) {
        return repository.findTenantIdById(input)
                .switchIfEmpty(Mono.error(() -> new ApplicationNotFoundException(input)));
    }
}
