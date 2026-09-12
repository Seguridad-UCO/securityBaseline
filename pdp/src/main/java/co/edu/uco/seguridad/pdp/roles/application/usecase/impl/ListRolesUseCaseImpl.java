package co.edu.uco.seguridad.pdp.roles.application.usecase.impl;

import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.ListRolesRequest;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.application.usecase.ListRolesUseCase;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Consulta paginada del catálogo visible para un inquilino. Delega y proyecta; el orden lo fija el adaptador. */
public final class ListRolesUseCaseImpl implements ListRolesUseCase {

    private final RoleRepository repository;

    public ListRolesUseCaseImpl(RoleRepository repository) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.ROLE_REPOSITORY);
    }

    @Override
    public Mono<ResultPage<RoleResponse>> execute(ListRolesRequest input) {
        return repository.findBy(input.criteria(), input.window())
                .map(page -> page.map(role ->
                        new RoleResponse(role.id(), role.name(), role.scope(), role.resources(), role.registeredAt())));
    }
}
