package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignProfileRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ProfileAssignmentResponse;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerProfileAssignmentCreationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerProfileAssignmentCreationUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AssignProfileRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ProfileAssignmentAdministrationWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerProfileAssignmentCreationInteractor;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
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
 * Implementación de {@link AdministerProfileAssignmentCreationInteractor} (HU-019). Construye el
 * {@code AdministrationRequest} directo desde {@code assignment.applicationId()}, sin lookup —
 * mismo criterio que {@code AdministerAssignmentCreationInteractorImpl} (HU-018).
 */
public final class AdministerProfileAssignmentCreationInteractorImpl implements AdministerProfileAssignmentCreationInteractor {

    private final AdministerProfileAssignmentCreationUseCase useCase;
    private final SubjectUserIdLookupValidator subjectUserIdLookup;

    public AdministerProfileAssignmentCreationInteractorImpl(AdministerProfileAssignmentCreationUseCase useCase,
                                                             SubjectUserIdLookupValidator subjectUserIdLookup) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.ADMINISTER_PROFILE_ASSIGNMENT_CREATION_USE_CASE);
        this.subjectUserIdLookup = Objects.requireNonNull(subjectUserIdLookup,
                RequiredArgumentMessages.SUBJECT_USER_ID_LOOKUP_VALIDATOR);
    }

    @Override
    public Mono<ProfileAssignmentAdministrationWebResponse> execute(AssignProfileRawRequest input) {
        return SecurityContext.currentPrincipal()
                .flatMap(principal -> resolveUserId(principal).map(userId -> toAdministerRequest(input, principal, userId)))
                .flatMap(useCase::execute)
                .map(AdministerProfileAssignmentCreationInteractorImpl::toWebResponse);
    }

    private static AdministerProfileAssignmentCreationRequest toAdministerRequest(AssignProfileRawRequest raw,
                                                                                  PdpPrincipal principal, UserId subjectUserId) {
        ProfileId profileId = RequestFieldParser.parse("profileId", raw.profileId(), ProfileId::of);
        UserId userId = RequestFieldParser.parse("userId", raw.userId(), UserId::of);
        ApplicationId applicationId = RequestFieldParser.parse("applicationId", raw.applicationId(), ApplicationId::of);
        AssignProfileRequest assignment = new AssignProfileRequest(principal.tenantId(), userId, applicationId, profileId);
        AdministrationRequest administration = new AdministrationRequest(principal.tenantId(), applicationId,
                subjectUserId, principal.subject(), Set.of(), principal.authenticationContext());
        return new AdministerProfileAssignmentCreationRequest(administration, assignment);
    }

    private static ProfileAssignmentAdministrationWebResponse toWebResponse(ProfileAssignmentResponse response) {
        List<String> generatedAssignmentIds = response.generatedAssignmentIds().stream()
                .map(id -> id.value().toString()).collect(Collectors.toList());
        return new ProfileAssignmentAdministrationWebResponse(response.id().value().toString(),
                response.userId().value().toString(), response.tenantId().value(),
                response.applicationId().value().toString(), response.profileId().value().toString(),
                generatedAssignmentIds, response.validFrom().toString(),
                response.validUntil().map(Object::toString).orElse(null));
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
