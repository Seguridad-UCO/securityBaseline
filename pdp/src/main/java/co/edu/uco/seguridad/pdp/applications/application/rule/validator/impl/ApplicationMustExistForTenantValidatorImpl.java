package co.edu.uco.seguridad.pdp.applications.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ApplicationOwnershipQuery;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationMustExistForTenantValidator;
import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.domain.rule.model.ApplicationExistence;
import co.edu.uco.seguridad.pdp.applications.domain.rule.ApplicationMustExistForTenantRule;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class ApplicationMustExistForTenantValidatorImpl implements ApplicationMustExistForTenantValidator {

    private final ApplicationRepository repository;
    private final ApplicationMustExistForTenantRule mustExist;

    public ApplicationMustExistForTenantValidatorImpl(ApplicationRepository repository,
                                                      ApplicationMustExistForTenantRule mustExist) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.APPLICATION_REPOSITORY);
        this.mustExist = Objects.requireNonNull(mustExist, RequiredArgumentMessages.APPLICATION_EXISTS_RULE);
    }

    @Override
    public Mono<Void> execute(ApplicationOwnershipQuery query) {
        return repository.existsByTenantAndId(query.tenantId(), query.applicationId())
                .doOnNext(registered -> mustExist.execute(
                        new ApplicationExistence(query.tenantId(), query.applicationId(), registered)))
                .then();
    }
}
