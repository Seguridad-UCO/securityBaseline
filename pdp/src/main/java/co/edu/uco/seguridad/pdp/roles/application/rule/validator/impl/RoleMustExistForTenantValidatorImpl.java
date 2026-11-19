package co.edu.uco.seguridad.pdp.roles.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.RoleOwnershipQuery;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleMustExistForTenantValidator;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.domain.rule.RoleMustExistForTenantRule;
import co.edu.uco.seguridad.pdp.roles.domain.rule.model.RoleExistence;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class RoleMustExistForTenantValidatorImpl implements RoleMustExistForTenantValidator {

    private final RoleRepository repository;
    private final RoleMustExistForTenantRule mustExist;

    public RoleMustExistForTenantValidatorImpl(RoleRepository repository, RoleMustExistForTenantRule mustExist) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.ROLE_REPOSITORY);
        this.mustExist = Objects.requireNonNull(mustExist, RequiredArgumentMessages.ROLE_EXISTS_RULE);
    }

    @Override
    public Mono<Void> execute(RoleOwnershipQuery query) {
        return repository.findByIdForTenant(query.roleId(), query.tenantId())
                .doOnNext(role -> mustExist.execute(new RoleExistence(query.roleId(), query.tenantId(), true)))
                .switchIfEmpty(Mono.fromRunnable(
                        () -> mustExist.execute(new RoleExistence(query.roleId(), query.tenantId(), false))))
                .then();
    }
}
