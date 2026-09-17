package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerProfileRoleAdditionRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerProfileRoleAdditionUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AddRoleToProfileRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ProfileAdministrationWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerProfileRoleAdditionInteractor;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.AddRoleToProfileRequest;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.ProfileOwnershipQuery;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.ProfileApplicationLookupValidator;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementación de {@link AdministerProfileRoleAdditionInteractor} (HU-019). Resuelve el
 * {@code applicationId} vía {@link ProfileApplicationLookupValidator} — la petición no lo trae
 * directo. Mismo criterio que {@code AdministerResourceGrantInteractorImpl} (HU-016).
 */
public final class AdministerProfileRoleAdditionInteractorImpl implements AdministerProfileRoleAdditionInteractor {

    private final AdministerProfileRoleAdditionUseCase useCase;
    private final SubjectUserIdLookupValidator subjectUserIdLookup;
    private final ProfileApplicationLookupValidator profileApplicationLookup;

    public AdministerProfileRoleAdditionInteractorImpl(AdministerProfileRoleAdditionUseCase useCase,
            SubjectUserIdLookupValidator subjectUserIdLookup, ProfileApplicationLookupValidator profileApplicationLookup) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.ADMINISTER_PROFILE_ROLE_ADDITION_USE_CASE);
        this.subjectUserIdLookup = Objects.requireNonNull(subjectUserIdLookup,
                RequiredArgumentMessages.SUBJECT_USER_ID_LOOKUP_VALIDATOR);
        this.profileApplicationLookup = Objects.requireNonNull(profileApplicationLookup,
                RequiredArgumentMessages.PROFILE_APPLICATION_LOOKUP_VALIDATOR);
    }

    @Override
    public Mono<ProfileAdministrationWebResponse> execute(AddRoleToProfileRawRequest input) {
        return SecurityContext.currentPrincipal()
                .flatMap(principal -> {
                    ProfileId profileId = RequestFieldParser.parse("profileId", input.profileId(), ProfileId::of);
                    RoleId roleId = RequestFieldParser.parse("roleId", input.roleId(), RoleId::of);
                    AddRoleToProfileRequest addition = new AddRoleToProfileRequest(principal.tenantId(), profileId, roleId);
                    return Mono.zip(resolveUserId(principal),
                            profileApplicationLookup.execute(new ProfileOwnershipQuery(principal.tenantId(), profileId)))
                            .map(tuple -> new AdministerProfileRoleAdditionRequest(
                                    tuple.getT2().map(applicationId -> new AdministrationRequest(principal.tenantId(),
                                            applicationId, tuple.getT1(), principal.subject(), Set.of(),
                                            principal.authenticationContext())),
                                    addition));
                })
                .flatMap(useCase::execute)
                .map(AdministerProfileRoleAdditionInteractorImpl::toWebResponse);
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
