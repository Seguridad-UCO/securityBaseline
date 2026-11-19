package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.TenantAdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustAdministerAtLeastOneApplicationValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerUserListingUseCase;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.response.UserResponse;
import co.edu.uco.seguridad.pdp.identity.application.usecase.ListUsersUseCase;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * El catálogo de usuarios de un inquilino solo lo ve quien administra al menos una aplicación de ese
 * inquilino (SU-01, mismo actor que ADR-036). Antes lo veía cualquier usuario autenticado, de todos los
 * inquilinos. Listar no se audita, mismo criterio que HU-020.
 */
public final class AdministerUserListingUseCaseImpl implements AdministerUserListingUseCase {

    private final PrincipalMustAdministerAtLeastOneApplicationValidator mustAdministerSomeApplication;
    private final ListUsersUseCase listUsers;

    public AdministerUserListingUseCaseImpl(PrincipalMustAdministerAtLeastOneApplicationValidator mustAdministerSomeApplication,
                                            ListUsersUseCase listUsers) {
        this.mustAdministerSomeApplication = Objects.requireNonNull(mustAdministerSomeApplication,
                RequiredArgumentMessages.PRINCIPAL_MUST_ADMINISTER_AT_LEAST_ONE_APPLICATION_VALIDATOR);
        this.listUsers = Objects.requireNonNull(listUsers, RequiredArgumentMessages.LIST_USERS_USE_CASE);
    }

    @Override
    public Mono<List<UserResponse>> execute(TenantAdministrationRequest actor) {
        return mustAdministerSomeApplication.execute(actor)
                .then(Mono.defer(() -> listUsers.execute(actor.tenantId())));
    }
}
