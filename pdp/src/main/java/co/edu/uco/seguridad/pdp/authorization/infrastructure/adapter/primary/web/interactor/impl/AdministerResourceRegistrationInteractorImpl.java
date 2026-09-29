package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerResourceRegistrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerResourceRegistrationUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.RegisterProtectedResourceRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.AdministeredResourceWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerResourceRegistrationInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.AdministeredResourceResponseMapper;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.RegisterProtectedResourceRequestMapper;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.RegisterProtectedResourceRequest;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Set;

/**
 * Implementación de {@link AdministerResourceRegistrationInteractor} (HU-017). Resuelve el principal
 * y su {@code UserId} (mismo patrón {@code resolveUserId} que
 * {@code AdministerRoleDefinitionInteractorImpl}), mapea el raw a
 * {@code RegisterProtectedResourceRequest} y construye el {@code AdministrationRequest}
 * directamente desde {@code resource.applicationId()} — sin {@code Optional}: siempre presente.
 */
public final class AdministerResourceRegistrationInteractorImpl implements AdministerResourceRegistrationInteractor {

    private final AdministerResourceRegistrationUseCase useCase;
    private final SubjectUserIdLookupValidator subjectUserIdLookup;

    public AdministerResourceRegistrationInteractorImpl(AdministerResourceRegistrationUseCase useCase,
                                                        SubjectUserIdLookupValidator subjectUserIdLookup) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.ADMINISTER_RESOURCE_REGISTRATION_USE_CASE);
        this.subjectUserIdLookup = Objects.requireNonNull(subjectUserIdLookup,
                RequiredArgumentMessages.SUBJECT_USER_ID_LOOKUP_VALIDATOR);
    }

    @Override
    public Mono<AdministeredResourceWebResponse> execute(RegisterProtectedResourceRawRequest input) {
        return SecurityContext.currentPrincipal()
                .flatMap(principal -> resolveUserId(principal)
                        .map(userId -> toAdministerRequest(input, principal, userId)))
                .flatMap(useCase::execute)
                .map(AdministeredResourceResponseMapper::toResponse);
    }

    private static AdministerResourceRegistrationRequest toAdministerRequest(RegisterProtectedResourceRawRequest raw,
                                                                             PdpPrincipal principal, UserId userId) {
        RegisterProtectedResourceRequest resource = RegisterProtectedResourceRequestMapper.toRequest(raw,
                principal.tenantId());
        AdministrationRequest administration = new AdministrationRequest(principal.tenantId(),
                resource.applicationId(), userId, principal.subject(), Set.of(), principal.authenticationContext());
        return new AdministerResourceRegistrationRequest(administration, resource);
    }

    private Mono<UserId> resolveUserId(PdpPrincipal principal) {
        return principal.userId()
                .map(Mono::just)
                .orElseGet(() -> subjectUserIdLookup.execute(principal.subject()))
                .switchIfEmpty(Mono.error(() -> new IllegalStateException(
                        "No fue posible resolver el UserId del llamador: el principal no lo trae y no hay "
                                + "ninguna identidad externa vinculada a su subject")));
    }
}
