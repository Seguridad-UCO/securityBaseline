package co.edu.uco.seguridad.pdp.roles.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.RoleCoverageQuery;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleScopeMustCoverApplicationValidator;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.domain.rule.RoleMustExistForTenantRule;
import co.edu.uco.seguridad.pdp.roles.domain.rule.RoleScopeMustCoverApplicationRule;
import co.edu.uco.seguridad.pdp.roles.domain.rule.model.ApplicationCoverage;
import co.edu.uco.seguridad.pdp.roles.domain.rule.model.RoleExistence;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class RoleScopeMustCoverApplicationValidatorImpl implements RoleScopeMustCoverApplicationValidator {

    private final RoleRepository repository;
    private final RoleMustExistForTenantRule roleMustExist;
    private final RoleScopeMustCoverApplicationRule coverageRule;

    public RoleScopeMustCoverApplicationValidatorImpl(RoleRepository repository, RoleMustExistForTenantRule roleMustExist,
            RoleScopeMustCoverApplicationRule coverageRule) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.ROLE_REPOSITORY);
        this.roleMustExist = Objects.requireNonNull(roleMustExist, RequiredArgumentMessages.ROLE_EXISTS_RULE);
        this.coverageRule = Objects.requireNonNull(coverageRule, RequiredArgumentMessages.ROLE_SCOPE_COVERS_APPLICATION_RULE);
    }

    @Override
    public Mono<Void> execute(RoleCoverageQuery input) {
        return repository.findById(input.roleId())
                .switchIfEmpty(Mono.fromRunnable(
                        () -> roleMustExist.execute(new RoleExistence(input.roleId(), input.tenantId(), false))))
                .doOnNext(role -> coverageRule.execute(new ApplicationCoverage(role.scope(), input.applicationId(), input.tenantId())))
                .then();
    }
}
