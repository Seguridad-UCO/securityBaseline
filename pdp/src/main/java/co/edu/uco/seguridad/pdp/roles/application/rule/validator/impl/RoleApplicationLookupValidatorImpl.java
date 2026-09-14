package co.edu.uco.seguridad.pdp.roles.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.RoleOwnershipQuery;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleApplicationLookupValidator;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.domain.rule.RoleMustExistForTenantRule;
import co.edu.uco.seguridad.pdp.roles.domain.rule.model.RoleExistence;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;

/**
 * Implementación de {@link RoleApplicationLookupValidator} (HU-016). Mismo patrón que
 * {@code RoleMustExistForTenantValidatorImpl}: resuelve el rol para el inquilino, deja que
 * {@link RoleMustExistForTenantRule} lance {@code RoleNotFoundException} si no existe, y aplana al
 * {@code applicationId} de su alcance (vacío para {@code TENANT}).
 */
public final class RoleApplicationLookupValidatorImpl implements RoleApplicationLookupValidator {

    private final RoleRepository repository;
    private final RoleMustExistForTenantRule mustExist;

    public RoleApplicationLookupValidatorImpl(RoleRepository repository, RoleMustExistForTenantRule mustExist) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.ROLE_REPOSITORY);
        this.mustExist = Objects.requireNonNull(mustExist, RequiredArgumentMessages.ROLE_EXISTS_RULE);
    }

    @Override
    public Mono<Optional<ApplicationId>> execute(RoleOwnershipQuery input) {
        return repository.findByIdForTenant(input.roleId(), input.tenantId())
                .doOnNext(role -> mustExist.execute(new RoleExistence(input.roleId(), input.tenantId(), true)))
                .switchIfEmpty(Mono.fromRunnable(
                        () -> mustExist.execute(new RoleExistence(input.roleId(), input.tenantId(), false))))
                .map(role -> role.scope().applicationId());
    }
}
