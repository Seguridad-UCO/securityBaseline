package co.edu.uco.seguridad.pdp.applications.application.rule.impl;

import co.edu.uco.seguridad.pdp.applications.application.exception.ApplicationNotFoundException;
import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ApplicationOwnershipQuery;
import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.application.rule.ApplicationMustExistForTenantRule;
import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class ApplicationMustExistForTenantRuleImpl implements ApplicationMustExistForTenantRule {

    private final ApplicationRepository repository;

    public ApplicationMustExistForTenantRuleImpl(ApplicationRepository repository) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.APPLICATION_REPOSITORY);
    }

    @Override
    public Mono<Application> execute(ApplicationOwnershipQuery query) {
        return repository.findByTenantAndId(query.tenantId(), query.applicationId())
                .switchIfEmpty(Mono.error(() -> new ApplicationNotFoundException(query.applicationId())));
    }
}
