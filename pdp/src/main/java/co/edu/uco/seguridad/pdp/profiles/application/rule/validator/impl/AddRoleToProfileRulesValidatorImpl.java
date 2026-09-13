package co.edu.uco.seguridad.pdp.profiles.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.AddRoleToProfileRequest;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.AddRoleToProfileRulesValidator;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import co.edu.uco.seguridad.pdp.profiles.domain.Profile;
import co.edu.uco.seguridad.pdp.profiles.domain.rule.ProfileMustExistForTenantRule;
import co.edu.uco.seguridad.pdp.profiles.domain.rule.model.ProfileExistence;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.RoleOwnershipQuery;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleMustExistForTenantValidator;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Hace la E/S para las reglas de concesión: el perfil existe para el inquilino (P2), y el rol
 * también existe para él (P3, validador prestado de {@code roles}). Devuelve el perfil encontrado
 * para que el caso de uso lo transforme. Espejo de GrantResourceRulesValidatorImpl.
 */
public final class AddRoleToProfileRulesValidatorImpl implements AddRoleToProfileRulesValidator {

    private final ProfileRepository repository;
    private final ProfileMustExistForTenantRule profileMustExist;
    private final RoleMustExistForTenantValidator roleMustExist;

    public AddRoleToProfileRulesValidatorImpl(ProfileRepository repository,
            ProfileMustExistForTenantRule profileMustExist, RoleMustExistForTenantValidator roleMustExist) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.PROFILE_REPOSITORY);
        this.profileMustExist = Objects.requireNonNull(profileMustExist, RequiredArgumentMessages.PROFILE_EXISTS_RULE);
        this.roleMustExist = Objects.requireNonNull(roleMustExist,
                RequiredArgumentMessages.ROLE_MUST_EXIST_FOR_TENANT_VALIDATOR);
    }

    @Override
    public Mono<Profile> execute(AddRoleToProfileRequest input) {
        return repository.findByIdForTenant(input.profileId(), input.tenantId())
                .doOnNext(profile -> profileMustExist.execute(new ProfileExistence(input.profileId(), input.tenantId(), true)))
                .switchIfEmpty(Mono.fromRunnable(() -> profileMustExist.execute(
                        new ProfileExistence(input.profileId(), input.tenantId(), false))))
                .flatMap(profile -> roleMustExist.execute(new RoleOwnershipQuery(input.roleId(), input.tenantId()))
                        .thenReturn(profile));
    }
}
