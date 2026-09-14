package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerResourceGrantRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerResourceGrantUseCase;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
import co.edu.uco.seguridad.pdp.roles.application.usecase.GrantResourceToRoleUseCase;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Implementación de {@link AdministerResourceGrantUseCase} (HU-016). Mismo patrón que
 * {@code AdministerRoleDefinitionUseCaseImpl} — gate condicional sobre {@code input.administration()}
 * seguido de {@code Mono.defer(() -> grantResource.execute(input.grant()))}.
 */
public final class AdministerResourceGrantUseCaseImpl implements AdministerResourceGrantUseCase {

    private final PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator;
    private final GrantResourceToRoleUseCase grantResource;

    public AdministerResourceGrantUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
            GrantResourceToRoleUseCase grantResource) {
        this.mustBeAdministrator = Objects.requireNonNull(mustBeAdministrator,
                RequiredArgumentMessages.PRINCIPAL_MUST_BE_APPLICATION_ADMINISTRATOR_VALIDATOR);
        this.grantResource = Objects.requireNonNull(grantResource, RequiredArgumentMessages.GRANT_RESOURCE_TO_ROLE_USE_CASE);
    }

    @Override
    public Mono<RoleResponse> execute(AdministerResourceGrantRequest input) {
        Mono<Void> gate = input.administration().map(mustBeAdministrator::execute).orElseGet(Mono::empty);
        return gate.then(Mono.defer(() -> grantResource.execute(input.grant())));
    }
}
