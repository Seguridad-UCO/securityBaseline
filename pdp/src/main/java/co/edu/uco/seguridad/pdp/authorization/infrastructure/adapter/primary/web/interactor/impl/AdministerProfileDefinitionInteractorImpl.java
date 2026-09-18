package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerProfileDefinitionRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerProfileDefinitionUseCase;
import co.edu.uco.seguridad.pdp.authorization.domain.message.AuthorizationMessages;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.DefineProfileRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ProfileAdministrationWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerProfileDefinitionInteractor;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.DefineProfileRequest;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse;
import co.edu.uco.seguridad.pdp.profiles.domain.model.ProfileName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScopeLevel;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementación de {@link AdministerProfileDefinitionInteractor} (HU-019). Construye
 * {@code Optional<AdministrationRequest>} directo desde el {@code RoleScope} de la petición — vacío
 * para alcance {@code TENANT}, sin lookup — mismo criterio que
 * {@code AdministerRoleDefinitionInteractorImpl} (HU-016).
 */
public final class AdministerProfileDefinitionInteractorImpl implements AdministerProfileDefinitionInteractor {

    private final AdministerProfileDefinitionUseCase useCase;
    private final SubjectUserIdLookupValidator subjectUserIdLookup;

    public AdministerProfileDefinitionInteractorImpl(AdministerProfileDefinitionUseCase useCase,
            SubjectUserIdLookupValidator subjectUserIdLookup) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.ADMINISTER_PROFILE_DEFINITION_USE_CASE);
        this.subjectUserIdLookup = Objects.requireNonNull(subjectUserIdLookup,
                RequiredArgumentMessages.SUBJECT_USER_ID_LOOKUP_VALIDATOR);
    }

    @Override
    public Mono<ProfileAdministrationWebResponse> execute(DefineProfileRawRequest input) {
        return SecurityContext.currentPrincipal()
                .flatMap(principal -> resolveUserId(principal).map(userId -> toAdministerRequest(input, principal, userId)))
                .flatMap(useCase::execute)
                .map(AdministerProfileDefinitionInteractorImpl::toWebResponse);
    }

    private static AdministerProfileDefinitionRequest toAdministerRequest(DefineProfileRawRequest raw,
            PdpPrincipal principal, UserId subjectUserId) {
        ProfileName name = RequestFieldParser.parse("name", raw.name(), ProfileName::new);
        RoleScopeLevel level = RequestFieldParser.parse("scope", raw.scope(), RoleScopeLevel::parse);

        if (level == RoleScopeLevel.GLOBAL) {
            throw new MalformedRequestFieldException("scope", AuthorizationMessages.globalProfileScopeNotAdministrableYet());
        }

        RoleScope scope = level == RoleScopeLevel.APPLICATION
                ? RoleScope.ofApplication(principal.tenantId(),
                        RequestFieldParser.parse("applicationId", raw.applicationId(), ApplicationId::of))
                : requireNoApplicationId(raw, principal.tenantId());

        DefineProfileRequest profile = new DefineProfileRequest(name, scope);
        Optional<AdministrationRequest> administration = scope.applicationId()
                .map(applicationId -> new AdministrationRequest(principal.tenantId(), applicationId, subjectUserId,
                        principal.subject(), Set.of(), principal.authenticationContext()));
        return new AdministerProfileDefinitionRequest(administration, profile);
    }

    private static RoleScope requireNoApplicationId(DefineProfileRawRequest raw, TenantId tenantId) {
        if (RequestFieldParser.optional(raw.applicationId()).isPresent()) {
            throw new MalformedRequestFieldException("applicationId",
                    AuthorizationMessages.applicationIdNotApplicableForProfileTenantScope());
        }
        return RoleScope.ofTenant(tenantId);
    }

    private static ProfileAdministrationWebResponse toWebResponse(ProfileResponse response) {
        List<String> roleIds = response.roles().stream().map(roleId -> roleId.value().toString()).collect(Collectors.toList());
        return new ProfileAdministrationWebResponse(response.id().value().toString(), response.name().value(),
                response.scope().level().name(), response.scope().tenantId().map(id -> id.value()).orElse(null),
                response.scope().applicationId().map(id -> id.value().toString()).orElse(null), roleIds,
                response.registeredAt().toString());
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
