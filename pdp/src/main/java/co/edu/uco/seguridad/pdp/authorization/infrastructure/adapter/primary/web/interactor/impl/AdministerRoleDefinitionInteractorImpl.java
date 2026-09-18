package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerRoleDefinitionRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerRoleDefinitionUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.DefineRoleRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.RoleAdministrationWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerRoleDefinitionInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.DefineRoleRequestMapper;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.RoleAdministrationResponseMapper;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.DefineRoleRequest;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Set;

/**
 * Implementación de {@link AdministerRoleDefinitionInteractor} (HU-016). Resuelve el principal y su
 * {@code UserId} (mismo patrón {@code resolveUserId} que {@code ApplicationRemovalInteractorImpl}),
 * mapea el raw a {@code DefineRoleRequest} y construye {@code Optional<AdministrationRequest>} a
 * partir de {@code role.scope().applicationId()} — presente solo si el alcance es
 * {@code APPLICATION}. El applicationId sale del propio scope de la petición, sin ninguna consulta
 * adicional (a diferencia de {@code AdministerResourceGrantInteractorImpl}).
 */
public final class AdministerRoleDefinitionInteractorImpl implements AdministerRoleDefinitionInteractor {

    private final AdministerRoleDefinitionUseCase useCase;
    private final SubjectUserIdLookupValidator subjectUserIdLookup;

    public AdministerRoleDefinitionInteractorImpl(AdministerRoleDefinitionUseCase useCase,
            SubjectUserIdLookupValidator subjectUserIdLookup) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.ADMINISTER_ROLE_DEFINITION_USE_CASE);
        this.subjectUserIdLookup = Objects.requireNonNull(subjectUserIdLookup,
                RequiredArgumentMessages.SUBJECT_USER_ID_LOOKUP_VALIDATOR);
    }

    @Override
    public Mono<RoleAdministrationWebResponse> execute(DefineRoleRawRequest input) {
        return SecurityContext.currentPrincipal()
                .flatMap(principal -> resolveUserId(principal)
                        .map(userId -> toAdministerRequest(input, principal, userId)))
                .flatMap(useCase::execute)
                .map(RoleAdministrationResponseMapper::toResponse);
    }

    private static AdministerRoleDefinitionRequest toAdministerRequest(DefineRoleRawRequest raw,
            PdpPrincipal principal, UserId userId) {
        DefineRoleRequest role = DefineRoleRequestMapper.toRequest(raw, principal.tenantId());
        var administration = role.scope().applicationId()
                .map(applicationId -> new AdministrationRequest(principal.tenantId(), applicationId, userId,
                        principal.subject(), Set.<String>of(), principal.authenticationContext()));
        return new AdministerRoleDefinitionRequest(administration, role);
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
