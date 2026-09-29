package co.edu.uco.seguridad.pdp.roles.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.ProtectedResourceOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.GrantResourceRequest;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.GrantResourceRulesValidator;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.domain.Role;
import co.edu.uco.seguridad.pdp.roles.domain.exception.ProtectedRoleException;
import co.edu.uco.seguridad.pdp.roles.domain.rule.RoleMustExistForTenantRule;
import co.edu.uco.seguridad.pdp.roles.domain.rule.RoleScopeMustCoverResourceRule;
import co.edu.uco.seguridad.pdp.roles.domain.rule.model.ResourceCoverage;
import co.edu.uco.seguridad.pdp.roles.domain.rule.model.RoleExistence;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Hace la E/S para las reglas de concesión, en orden de coste: el rol para el inquilino (R3), la
 * aplicación dueña del recurso (R4, validador prestado de {@code resources}), el inquilino de esa
 * aplicación (validador prestado de {@code applications}), y por último la cobertura del alcance
 * (R5, regla pura). Devuelve el rol encontrado para que el caso de uso lo transforme.
 */
public final class GrantResourceRulesValidatorImpl implements GrantResourceRulesValidator {

    private static final String ADMIN = "ADMIN";

    private final RoleRepository repository;
    private final RoleMustExistForTenantRule roleMustExist;
    private final ProtectedResourceOwnerLookupValidator resourceOwner;
    private final ApplicationOwnerLookupValidator applicationOwner;
    private final RoleScopeMustCoverResourceRule scopeMustCover;

    public GrantResourceRulesValidatorImpl(RoleRepository repository, RoleMustExistForTenantRule roleMustExist,
            ProtectedResourceOwnerLookupValidator resourceOwner, ApplicationOwnerLookupValidator applicationOwner,
            RoleScopeMustCoverResourceRule scopeMustCover) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.ROLE_REPOSITORY);
        this.roleMustExist = Objects.requireNonNull(roleMustExist, RequiredArgumentMessages.ROLE_EXISTS_RULE);
        this.resourceOwner = Objects.requireNonNull(resourceOwner,
                RequiredArgumentMessages.PROTECTED_RESOURCE_OWNER_LOOKUP_VALIDATOR);
        this.applicationOwner = Objects.requireNonNull(applicationOwner,
                RequiredArgumentMessages.APPLICATION_OWNER_LOOKUP_VALIDATOR);
        this.scopeMustCover = Objects.requireNonNull(scopeMustCover, RequiredArgumentMessages.ROLE_SCOPE_COVERS_RULE);
    }

    @Override
    public Mono<Role> execute(GrantResourceRequest input) {
        return repository.findByIdForTenant(input.roleId(), input.tenantId())
                .doOnNext(role -> roleMustExist.execute(new RoleExistence(input.roleId(), input.tenantId(), true)))
                .switchIfEmpty(Mono.fromRunnable(
                        () -> roleMustExist.execute(new RoleExistence(input.roleId(), input.tenantId(), false))))
                .flatMap(role -> isDefaultApplicationAdministrator(role)
                        ? Mono.error(new ProtectedRoleException(role.id()))
                        : resourceOwner.execute(input.resourceId())
                        .flatMap(resourceApplicationId -> applicationOwner.execute(resourceApplicationId)
                                .doOnNext(resourceTenantId -> scopeMustCover.execute(
                                        new ResourceCoverage(role.scope(), resourceApplicationId, resourceTenantId)))
                                .thenReturn(role)));
    }

    private static boolean isDefaultApplicationAdministrator(Role role) {
        return ADMIN.equalsIgnoreCase(role.name().value()) && role.scope().applicationId().isPresent();
    }
}
