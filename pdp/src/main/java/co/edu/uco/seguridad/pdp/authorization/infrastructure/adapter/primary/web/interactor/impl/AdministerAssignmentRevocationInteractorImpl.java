package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignmentOwnershipQuery;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RevokeAssignmentRequest;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.AssignmentApplicationLookupValidator;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerAssignmentRevocationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerAssignmentRevocationUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.RevokeAssignmentRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerAssignmentRevocationInteractor;
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
 * Implementación de {@link AdministerAssignmentRevocationInteractor} (HU-018). Resuelve el
 * {@code applicationId} vía {@link AssignmentApplicationLookupValidator} — la petición de
 * revocación no lo trae directo.
 */
public final class AdministerAssignmentRevocationInteractorImpl implements AdministerAssignmentRevocationInteractor {

    private final AdministerAssignmentRevocationUseCase useCase;
    private final SubjectUserIdLookupValidator subjectUserIdLookup;
    private final AssignmentApplicationLookupValidator applicationLookup;

    public AdministerAssignmentRevocationInteractorImpl(AdministerAssignmentRevocationUseCase useCase,
            SubjectUserIdLookupValidator subjectUserIdLookup, AssignmentApplicationLookupValidator applicationLookup) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.ADMINISTER_ASSIGNMENT_REVOCATION_USE_CASE);
        this.subjectUserIdLookup = Objects.requireNonNull(subjectUserIdLookup,
                RequiredArgumentMessages.SUBJECT_USER_ID_LOOKUP_VALIDATOR);
        this.applicationLookup = Objects.requireNonNull(applicationLookup,
                RequiredArgumentMessages.ASSIGNMENT_APPLICATION_LOOKUP_VALIDATOR);
    }

    @Override
    public Mono<Void> execute(RevokeAssignmentRawRequest input) {
        return SecurityContext.currentPrincipal()
                .flatMap(principal -> {
                    AssignmentId assignmentId = RequestFieldParser.parse("assignmentId", input.assignmentId(), AssignmentId::of);
                    RevokeAssignmentRequest revocation = new RevokeAssignmentRequest(assignmentId, principal.tenantId());
                    return Mono.zip(resolveUserId(principal),
                            applicationLookup.execute(new AssignmentOwnershipQuery(assignmentId, principal.tenantId())))
                            .map(tuple -> new AdministerAssignmentRevocationRequest(
                                    new AdministrationRequest(principal.tenantId(), tuple.getT2(), tuple.getT1(),
                                            principal.subject(), Set.of()),
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
