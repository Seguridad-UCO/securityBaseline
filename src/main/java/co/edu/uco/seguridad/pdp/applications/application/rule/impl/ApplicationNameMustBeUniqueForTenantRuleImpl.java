package co.edu.uco.seguridad.pdp.applications.application.rule.impl;

import co.edu.uco.seguridad.pdp.applications.application.exception.DuplicateApplicationException;
import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.application.rule.ApplicationNameMustBeUniqueForTenantRule;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class ApplicationNameMustBeUniqueForTenantRuleImpl implements ApplicationNameMustBeUniqueForTenantRule {

    private final ApplicationRepository repository;

    public ApplicationNameMustBeUniqueForTenantRuleImpl(ApplicationRepository repository) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.APPLICATION_REPOSITORY);
    }

    @Override
    public Mono<Void> execute(RegisterApplicationRequest dto) {
        return repository.existsByTenantAndName(dto.tenantId(), dto.name())
                .filter(Boolean::booleanValue)
                .flatMap(exists -> Mono.<Void>error(
                        new DuplicateApplicationException(dto.tenantId(), dto.name())));
    }
}
