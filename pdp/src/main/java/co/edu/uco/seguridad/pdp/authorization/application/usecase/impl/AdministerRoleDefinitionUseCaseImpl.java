package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerRoleDefinitionRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerRoleDefinitionUseCase;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
import co.edu.uco.seguridad.pdp.roles.application.usecase.DefineRoleUseCase;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Implementación de {@link AdministerRoleDefinitionUseCase} (HU-016). Gatea solo si
 * {@code administration} está presente (rol de alcance {@code APPLICATION}) — un rol
 * {@code TENANT} delega directo, sin gate. Si el validador rechaza, termina en
 * {@code NotAuthorizedToAdministerException} sin llegar a invocar {@code DefineRoleUseCase}.
 */
public final class AdministerRoleDefinitionUseCaseImpl implements AdministerRoleDefinitionUseCase {

    private final PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator;
    private final DefineRoleUseCase defineRole;

    public AdministerRoleDefinitionUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
            DefineRoleUseCase defineRole) {
        this.mustBeAdministrator = Objects.requireNonNull(mustBeAdministrator,
                RequiredArgumentMessages.PRINCIPAL_MUST_BE_APPLICATION_ADMINISTRATOR_VALIDATOR);
        this.defineRole = Objects.requireNonNull(defineRole, RequiredArgumentMessages.DEFINE_ROLE_USE_CASE);
    }

    @Override
    public Mono<RoleResponse> execute(AdministerRoleDefinitionRequest input) {
        Mono<Void> gate = input.administration().map(mustBeAdministrator::execute).orElseGet(Mono::empty);
        return gate.then(Mono.defer(() -> defineRole.execute(input.role())));
    }
}
