package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerUserTenantAssignmentRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerUserTenantAssignmentUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AssignTenantRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.UserWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerUserTenantAssignmentInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.AssignTenantRequestMapper;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.UserResponseMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class AdministerUserTenantAssignmentInteractorImpl implements AdministerUserTenantAssignmentInteractor {

    private final AdministerUserTenantAssignmentUseCase useCase;
    private final TenantActorResolver actors;

    public AdministerUserTenantAssignmentInteractorImpl(AdministerUserTenantAssignmentUseCase useCase, TenantActorResolver actors) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.ADMINISTER_USER_TENANT_ASSIGNMENT_USE_CASE);
        this.actors = Objects.requireNonNull(actors, RequiredArgumentMessages.SUBJECT_USER_ID_LOOKUP_VALIDATOR);
    }

    @Override
    public Mono<UserWebResponse> execute(AssignTenantRawRequest raw) {
        return SecurityContext.currentPrincipal()
                .flatMap(actors::resolve)
                .map(actor -> new AdministerUserTenantAssignmentRequest(actor, AssignTenantRequestMapper.toRequest(raw)))
                .flatMap(useCase::execute)
                .map(UserResponseMapper::toResponse);
    }
}
