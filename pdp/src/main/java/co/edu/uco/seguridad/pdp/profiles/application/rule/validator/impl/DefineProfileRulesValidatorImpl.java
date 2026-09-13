package co.edu.uco.seguridad.pdp.profiles.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ApplicationOwnershipQuery;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationMustExistForTenantValidator;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.DefineProfileRequest;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.DefineProfileRulesValidator;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import co.edu.uco.seguridad.pdp.profiles.domain.rule.ProfileNameMustBeUniqueInScopeRule;
import co.edu.uco.seguridad.pdp.profiles.domain.rule.model.ProfileNameAvailability;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Hace la E/S para las reglas de definición: si el alcance trae aplicación, la verifica con el
 * validador prestado de {@code applications} (P... ver PLAN-HU-011.md §3); después resuelve la
 * disponibilidad del nombre y deja decidir a la regla pura. Espejo de DefineRoleRulesValidatorImpl.
 */
public final class DefineProfileRulesValidatorImpl implements DefineProfileRulesValidator {

    private final ApplicationMustExistForTenantValidator applicationMustExist;
    private final ProfileNameMustBeUniqueInScopeRule nameMustBeUnique;
    private final ProfileRepository repository;

    public DefineProfileRulesValidatorImpl(ApplicationMustExistForTenantValidator applicationMustExist,
            ProfileNameMustBeUniqueInScopeRule nameMustBeUnique, ProfileRepository repository) {
        this.applicationMustExist = Objects.requireNonNull(applicationMustExist,
                RequiredArgumentMessages.APPLICATION_EXISTS_VALIDATOR);
        this.nameMustBeUnique = Objects.requireNonNull(nameMustBeUnique, RequiredArgumentMessages.PROFILE_NAME_UNIQUE_RULE);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.PROFILE_REPOSITORY);
    }

    @Override
    public Mono<Void> execute(DefineProfileRequest input) {
        RoleScope scope = input.scope();
        Mono<Void> applicationExists = scope.applicationId()
                .map(applicationId -> applicationMustExist.execute(
                        new ApplicationOwnershipQuery(scope.tenantId().orElseThrow(), applicationId)))
                .orElse(Mono.empty());

        return applicationExists.then(Mono.defer(() -> repository.existsByNameInScope(input.name(), scope)
                .doOnNext(taken -> nameMustBeUnique.execute(new ProfileNameAvailability(input.name(), scope, taken)))
                .then()));
    }
}
