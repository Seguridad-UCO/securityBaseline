package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerResourceGrantRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerResourceGrantUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.GrantResourceRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.RoleAdministrationWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerResourceGrantInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.GrantResourceRequestMapper;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.RoleAdministrationResponseMapper;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.GrantResourceRequest;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.RoleOwnershipQuery;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleApplicationLookupValidator;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Set;

/**
 * Implementación de {@link AdministerResourceGrantInteractor} (HU-016). A diferencia de
 * {@code AdministerRoleDefinitionInteractorImpl}, el applicationId no viene en la petición — se
 * resuelve del rol ya existente vía {@link RoleApplicationLookupValidator}, que también propaga
 * {@code RoleNotFoundException} si el rol no existe para el inquilino, antes de construir el
 * {@code Optional<AdministrationRequest>}.
 */
public final class AdministerResourceGrantInteractorImpl implements AdministerResourceGrantInteractor {

    private final AdministerResourceGrantUseCase useCase;
    private final SubjectUserIdLookupValidator subjectUserIdLookup;
    private final RoleApplicationLookupValidator roleApplicationLookup;

    public AdministerResourceGrantInteractorImpl(AdministerResourceGrantUseCase useCase,
            SubjectUserIdLookupValidator subjectUserIdLookup, RoleApplicationLookupValidator roleApplicationLookup) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.ADMINISTER_RESOURCE_GRANT_USE_CASE);
        this.subjectUserIdLookup = Objects.requireNonNull(subjectUserIdLookup,
                RequiredArgumentMessages.SUBJECT_USER_ID_LOOKUP_VALIDATOR);
        this.roleApplicationLookup = Objects.requireNonNull(roleApplicationLookup,
                RequiredArgumentMessages.ROLE_APPLICATION_LOOKUP_VALIDATOR);
    }

    @Override
    public Mono<RoleAdministrationWebResponse> execute(GrantResourceRawRequest input) {
        return SecurityContext.currentPrincipal()
                .flatMap(principal -> resolveUserId(principal)
                        .flatMap(userId -> toAdministerRequest(input, principal, userId)))
                .flatMap(useCase::execute)
                .map(RoleAdministrationResponseMapper::toResponse);
    }

    private Mono<AdministerResourceGrantRequest> toAdministerRequest(GrantResourceRawRequest raw,
            PdpPrincipal principal, UserId userId) {
        GrantResourceRequest grant = GrantResourceRequestMapper.toRequest(raw, principal.tenantId());
        return roleApplicationLookup.execute(new RoleOwnershipQuery(grant.roleId(), principal.tenantId()))
                .map(maybeApplicationId -> maybeApplicationId
                        .map(applicationId -> new AdministrationRequest(principal.tenantId(), applicationId, userId,
                                principal.subject(), Set.<String>of())))
                .map(administration -> new AdministerResourceGrantRequest(administration, grant));
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
