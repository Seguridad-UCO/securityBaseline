package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ProfileAssignmentOwnershipQuery;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RevokeProfileAssignmentRequest;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.ProfileAssignmentApplicationLookupValidator;
import co.edu.uco.seguridad.pdp.assignments.domain.model.ProfileAssignmentId;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerProfileAssignmentRevocationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerProfileAssignmentRevocationUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.RevokeProfileAssignmentRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerProfileAssignmentRevocationInteractor;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Set;

/**
 * Implementación de {@link AdministerProfileAssignmentRevocationInteractor} (HU-019). Resuelve el
 * {@code applicationId} vía {@link ProfileAssignmentApplicationLookupValidator} — mismo criterio
 * que {@code AdministerAssignmentRevocationInteractorImpl} (HU-018).
 */
public final class AdministerProfileAssignmentRevocationInteractorImpl implements AdministerProfileAssignmentRevocationInteractor {

    private final AdministerProfileAssignmentRevocationUseCase useCase;
    private final SubjectUserIdLookupValidator subjectUserIdLookup;
    private final ProfileAssignmentApplicationLookupValidator applicationLookup;

    public AdministerProfileAssignmentRevocationInteractorImpl(AdministerProfileAssignmentRevocationUseCase useCase,
            SubjectUserIdLookupValidator subjectUserIdLookup, ProfileAssignmentApplicationLookupValidator applicationLookup) {
        this.useCase = Objects.requireNonNull(useCase,
                RequiredArgumentMessages.ADMINISTER_PROFILE_ASSIGNMENT_REVOCATION_USE_CASE);
        this.subjectUserIdLookup = Objects.requireNonNull(subjectUserIdLookup,
                RequiredArgumentMessages.SUBJECT_USER_ID_LOOKUP_VALIDATOR);
        this.applicationLookup = Objects.requireNonNull(applicationLookup,
                RequiredArgumentMessages.PROFILE_ASSIGNMENT_APPLICATION_LOOKUP_VALIDATOR);
    }

    @Override
    public Mono<Void> execute(RevokeProfileAssignmentRawRequest input) {
        return SecurityContext.currentPrincipal()
                .flatMap(principal -> {
                    ProfileAssignmentId profileAssignmentId = RequestFieldParser.parse("profileAssignmentId",
                            input.profileAssignmentId(), ProfileAssignmentId::of);
                    RevokeProfileAssignmentRequest revocation =
                            new RevokeProfileAssignmentRequest(profileAssignmentId, principal.tenantId());
                    return Mono.zip(resolveUserId(principal), applicationLookup.execute(
                                    new ProfileAssignmentOwnershipQuery(profileAssignmentId, principal.tenantId())))
                            .map(tuple -> new AdministerProfileAssignmentRevocationRequest(
                                    new AdministrationRequest(principal.tenantId(), tuple.getT2(), tuple.getT1(),
                                            principal.subject(), Set.of(), principal.authenticationContext()),
                                    revocation));
                })
                .flatMap(useCase::execute);
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
