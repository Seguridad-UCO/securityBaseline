package co.edu.uco.seguridad.pdp.roles.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ApplicationOwnershipQuery;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationMustExistForTenantValidator;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.DefineRoleRequest;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.DefineRoleRulesValidator;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.pdp.roles.domain.rule.RoleNameMustBeUniqueInScopeRule;
import co.edu.uco.seguridad.pdp.roles.domain.rule.model.RoleNameAvailability;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Hace la E/S para las reglas de definición: si el alcance trae aplicación, la verifica con el
 * validador prestado de {@code applications} (R2); después resuelve la disponibilidad del nombre y
 * deja decidir a la regla pura (R1). El {@code if} de orquestación sobre el {@code Optional} del
 * alcance se expresa con {@code Mono.justOrEmpty}, no con una rama.
 */
public final class DefineRoleRulesValidatorImpl implements DefineRoleRulesValidator {

    private final ApplicationMustExistForTenantValidator applicationMustExist;
    private final RoleNameMustBeUniqueInScopeRule nameMustBeUnique;
    private final RoleRepository repository;

    public DefineRoleRulesValidatorImpl(ApplicationMustExistForTenantValidator applicationMustExist,
                                        RoleNameMustBeUniqueInScopeRule nameMustBeUnique, RoleRepository repository) {
        this.applicationMustExist = Objects.requireNonNull(applicationMustExist,
                RequiredArgumentMessages.APPLICATION_EXISTS_VALIDATOR);
        this.nameMustBeUnique = Objects.requireNonNull(nameMustBeUnique, RequiredArgumentMessages.ROLE_NAME_UNIQUE_RULE);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.ROLE_REPOSITORY);
    }

    @Override
    public Mono<Void> execute(DefineRoleRequest input) {
        RoleScope scope = input.scope();
        Mono<Void> applicationExists = scope.applicationId()
                .map(applicationId -> applicationMustExist.execute(
                        new ApplicationOwnershipQuery(scope.tenantId().orElseThrow(), applicationId)))
                .orElse(Mono.empty());

        return applicationExists.then(Mono.defer(() -> repository.existsByNameInScope(input.name(), scope)
                .doOnNext(taken -> nameMustBeUnique.execute(new RoleNameAvailability(input.name(), scope, taken)))
                .then()));
    }
}
