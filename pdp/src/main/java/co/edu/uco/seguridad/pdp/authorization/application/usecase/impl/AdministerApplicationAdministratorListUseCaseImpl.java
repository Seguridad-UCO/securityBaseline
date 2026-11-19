package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ListApplicationAdministratorsUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerApplicationAdministratorListRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerApplicationAdministratorListUseCase;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

public final class AdministerApplicationAdministratorListUseCaseImpl
        implements AdministerApplicationAdministratorListUseCase {

    private final PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator;
    private final ListApplicationAdministratorsUseCase listApplicationAdministrators;

    public AdministerApplicationAdministratorListUseCaseImpl(
            PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
            ListApplicationAdministratorsUseCase listApplicationAdministrators) {
        this.mustBeAdministrator = Objects.requireNonNull(mustBeAdministrator,
                RequiredArgumentMessages.PRINCIPAL_MUST_BE_APPLICATION_ADMINISTRATOR_VALIDATOR);
        this.listApplicationAdministrators = Objects.requireNonNull(listApplicationAdministrators,
                RequiredArgumentMessages.LIST_APPLICATION_ADMINISTRATORS_USE_CASE);
    }

    @Override
    public Mono<List<AssignmentResponse>> execute(AdministerApplicationAdministratorListRequest input) {
        return mustBeAdministrator.execute(input.administration())
                .then(Mono.defer(() -> listApplicationAdministrators.execute(input.query())));
    }
}
